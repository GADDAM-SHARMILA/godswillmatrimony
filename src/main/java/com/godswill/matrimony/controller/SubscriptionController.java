package com.godswill.matrimony.controller;

import com.godswill.matrimony.model.Profile;
import com.godswill.matrimony.model.Subscription;
import com.godswill.matrimony.model.SubscriptionPlan;
import com.godswill.matrimony.model.User;
import com.godswill.matrimony.service.PaymentService;
import com.godswill.matrimony.service.ProfileService;
import com.godswill.matrimony.service.SubscriptionService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.razorpay.RazorpayException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final ProfileService profileService;

    /* =====================================================
       SHOW PLANS
    ===================================================== */

    @GetMapping("/plans")
    public String showPremiumPlans(HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to view premium plans");
            return "redirect:/login";
        }

        model.addAttribute("plans", SubscriptionPlan.values());

        Optional<Subscription> activeSub =
                subscriptionService.getActiveSubscription(user.getId());

        model.addAttribute("activeSubscription", activeSub.orElse(null));

        return "user/premium-plans";
    }

    /* =====================================================
       CREATE ORDER
    ===================================================== */

    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<?> createOrder(@RequestParam("plan") String planName,
                                         HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not logged in");
            error.put("redirect", "/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            SubscriptionPlan plan =
                    SubscriptionPlan.valueOf(planName.toUpperCase());

            Map<String, String> orderData =
                    paymentService.createOrder(plan, user.getId());

            subscriptionService.createSubscription(
                    user.getId(),
                    plan,
                    orderData.get("orderId")
            );

            return ResponseEntity.ok(orderData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid plan selected"));
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order"));
        }
    }

    /* =====================================================
       VERIFY PAYMENT
    ===================================================== */

    @PostMapping("/verify-payment")
    public String verifyPayment(
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Session expired. Please login again.");
            return "redirect:/login";
        }

        try {
            Optional<Subscription> pendingSub =
                    subscriptionService.getSubscriptionByOrderId(orderId);

            if (pendingSub.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Subscription not found.");
                return "redirect:/subscription/failed";
            }

            Subscription subscription = pendingSub.get();

            boolean isValid = paymentService.verifyAndProcessPayment(
                    orderId, paymentId, signature,
                    user.getEmail(), user.getFirstName(), subscription.getPlan()
            );

            if (isValid) {
                Subscription activated = subscriptionService.activateSubscription(
                        orderId, paymentId, signature);

                user.setActiveSubscriptionId(activated.getId());
                user.setCurrentPlan(activated.getPlan());
                user.setSubscriptionExpiryDate(activated.getEndDate());
                session.setAttribute("user", user);

                redirectAttributes.addFlashAttribute("success", "Subscription activated successfully!");
                return "redirect:/subscription/success";

            } else {
                subscriptionService.markPaymentFailed(orderId, "Invalid signature");
                redirectAttributes.addFlashAttribute("error", "Payment verification failed.");
                return "redirect:/subscription/failed";
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred during payment verification.");
            return "redirect:/subscription/failed";
        }
    }

    /* =====================================================
       SUCCESS PAGE
    ===================================================== */

    @GetMapping("/success")
    public String paymentSuccess(HttpSession session, Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to continue");
            return "redirect:/login";
        }
        Optional<Subscription> activeSub = subscriptionService.getActiveSubscription(user.getId());
        model.addAttribute("subscription", activeSub.orElse(null));
        return "user/payment-success";
    }

    /* =====================================================
       FAILED PAGE
    ===================================================== */

    @GetMapping("/failed")
    public String paymentFailed(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to continue");
            return "redirect:/login";
        }
        return "user/payment-failed";
    }

    /* =====================================================
       DOWNLOAD RECEIPT AS PDF
       — Logo aligned RIGHT in header
       — Diagonal "God's Will Matrimony" watermark behind content
       FIX: Do NOT call document.close() before the watermark loop.
            Instead flush the document layout, draw watermark, then close pdfDoc.
    ===================================================== */

    @GetMapping("/receipt/{subscriptionId}")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable String subscriptionId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Subscription> subOpt = subscriptionService.getSubscriptionById(subscriptionId);
        if (subOpt.isEmpty() || !subOpt.get().getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Subscription sub = subOpt.get();

        // profileNumber — same field shown on profile-details page as "Profile ID: GWM-1"
        Optional<Profile> profileOpt = profileService.getProfileByUserId(user.getId());
        String profileNumber = profileOpt
                .map(Profile::getProfileNumber)
                .filter(pn -> pn != null && !pn.isBlank())
                .orElse("—");

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);

            // ── KEY FIX: use Document(pdfDoc) — we will NOT call document.close().
            //    We call document.flush() + pdfDoc.close() ourselves after the watermark. ──
            Document document = new Document(pdfDoc);

            PdfFont boldFont    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            DeviceRgb brandRed  = new DeviceRgb(180, 80, 110); // #B4506E — darkened F2D8E1 for contrast
            DeviceRgb lightGray = new DeviceRgb(120, 120, 120);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

            /* =================================================
               HEADER ROW: Brand title LEFT  |  Logo RIGHT
            ================================================= */

            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setMarginBottom(4);

            // Left cell — brand name + subtitle
            Cell brandCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .add(new Paragraph("God's Will Matrimony")
                            .setFont(boldFont)
                            .setFontSize(22)
                            .setFontColor(brandRed)
                            .setMarginBottom(2))
                    .add(new Paragraph("Official Payment Receipt")
                            .setFont(regularFont)
                            .setFontSize(11)
                            .setFontColor(lightGray));
            headerTable.addCell(brandCell);

            // Right cell — logo aligned to right
            Cell logoCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .setTextAlignment(TextAlignment.RIGHT);

            try {
                Path logoPath = Paths.get("src/main/resources/static/images/logo.png");
                if (Files.exists(logoPath)) {
                    byte[] logoBytes = Files.readAllBytes(logoPath);
                    Image logo = new Image(ImageDataFactory.create(logoBytes));
                    logo.setWidth(80);
                    logo.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                    logoCell.add(logo);
                }
            } catch (Exception ignored) {
                // Logo missing — cell stays empty, layout unaffected
            }

            headerTable.addCell(logoCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));
            document.add(new LineSeparator(new SolidLine()));
            document.add(new Paragraph(" "));

            /* =================================================
               CUSTOMER DETAILS
            ================================================= */

            document.add(new Paragraph("Customer Details")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setFontColor(ColorConstants.BLACK));

            String[][] customerRows = {
                    {"Name",       user.getFirstName() + " " + user.getLastName()},
                    {"Email",      user.getEmail() != null ? user.getEmail() : "—"},
                    {"Profile ID", profileNumber},
            };

            Table customerTable = new Table(UnitValue.createPercentArray(new float[]{35, 65}));
            customerTable.setWidth(UnitValue.createPercentValue(100));
            for (String[] row : customerRows) {
                customerTable.addCell(
                        new Cell().add(new Paragraph(row[0]).setFont(boldFont).setFontSize(11)));
                customerTable.addCell(
                        new Cell().add(new Paragraph(row[1]).setFont(regularFont).setFontSize(11)));
            }
            document.add(customerTable);

            document.add(new Paragraph(" "));

            /* =================================================
               PAYMENT DETAILS
            ================================================= */

            document.add(new Paragraph("Payment Details")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setFontColor(ColorConstants.BLACK));

            String[][] paymentRows = {
                    {"Receipt ID",     sub.getId() != null ? sub.getId() : "—"},
                    {"Plan",           sub.getPlan() != null ? sub.getPlan().name() : "—"},
                    {"Amount Paid",    sub.getAmount() != null ? "Rs. " + sub.getAmount() : "—"},
                    {"Payment Status", sub.getPaymentStatus() != null ? sub.getPaymentStatus() : "—"},
                    {"Payment ID",     sub.getRazorpayPaymentId() != null ? sub.getRazorpayPaymentId() : "—"},
                    {"Order ID",       sub.getRazorpayOrderId() != null ? sub.getRazorpayOrderId() : "—"},
                    {"Start Date",     sub.getStartDate() != null ? sub.getStartDate().format(formatter) : "—"},
                    {"Expiry Date",    sub.getEndDate()   != null ? sub.getEndDate().format(formatter)   : "—"},
            };

            Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{35, 65}));
            paymentTable.setWidth(UnitValue.createPercentValue(100));
            DeviceRgb successGreen = new DeviceRgb(34, 139, 34);
            for (String[] row : paymentRows) {
                paymentTable.addCell(
                        new Cell().add(new Paragraph(row[0]).setFont(boldFont).setFontSize(11)));
                boolean isSuccess = row[0].equals("Payment Status") &&
                        (row[1].equalsIgnoreCase("SUCCESS") ||
                                row[1].equalsIgnoreCase("PAID") ||
                                row[1].equalsIgnoreCase("CAPTURED"));
                Paragraph valueP = isSuccess
                        ? new Paragraph(row[1]).setFont(boldFont).setFontSize(11).setFontColor(successGreen)
                        : new Paragraph(row[1]).setFont(regularFont).setFontSize(11);
                paymentTable.addCell(new Cell().add(valueP));
            }
            document.add(paymentTable);

            document.add(new Paragraph(" "));
            document.add(new LineSeparator(new SolidLine()));
            document.add(new Paragraph(" "));

            /* =================================================
               FOOTER
            ================================================= */

            document.add(new Paragraph("Thank you for choosing God's Will Matrimony!")
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setFontColor(brandRed)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("This is a computer-generated receipt and does not require a signature.")
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(lightGray)
                    .setTextAlignment(TextAlignment.CENTER));

            /* =================================================
               WATERMARK
               ✅ FIX: flush document layout FIRST (does NOT close pdfDoc),
                       then draw watermark on each page,
                       then close pdfDoc ourselves.
            ================================================= */

            document.flush(); // writes all layout elements to pages — pdfDoc stays OPEN

            PdfFont watermarkFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            int totalPages = pdfDoc.getNumberOfPages(); // ✅ now works — pdfDoc is still open

            for (int i = 1; i <= totalPages; i++) {
                PdfPage page = pdfDoc.getPage(i);
                Rectangle pageSize = page.getPageSize();

                // Draw BEHIND existing content using newContentStreamBefore
                PdfCanvas pdfCanvas = new PdfCanvas(
                        page.newContentStreamBefore(),
                        page.getResources(),
                        pdfDoc
                );

                // Semi-transparent graphics state
                PdfExtGState gs = new PdfExtGState();
                gs.setFillOpacity(0.08f); // subtle — raise to 0.12f if you want it more visible

                pdfCanvas.saveState();
                pdfCanvas.setExtGState(gs);
                pdfCanvas.setFillColor(new DeviceRgb(196, 30, 58)); // brand red

                // Rotate 45° around page center
                float cx = pageSize.getWidth()  / 2;
                float cy = pageSize.getHeight() / 2;

                pdfCanvas.concatMatrix(
                        Math.cos(Math.toRadians(45)),
                        Math.sin(Math.toRadians(45)),
                        -Math.sin(Math.toRadians(45)),
                        Math.cos(Math.toRadians(45)),
                        cx, cy
                );

                // Draw watermark text centered at the rotated origin
                pdfCanvas.beginText()
                        .setFontAndSize(watermarkFont, 52)
                        .moveText(-200, -18)
                        .showText("God's Will Matrimony")
                        .endText();

                pdfCanvas.restoreState();
                pdfCanvas.release();
            }

            // ✅ Close pdfDoc (not document) — this finalises the PDF into the ByteArrayOutputStream
            pdfDoc.close();

            byte[] pdfBytes = baos.toByteArray();

            // Filename: FirstName_GodswillMatrimony.pdf
            String safeFirstName = user.getFirstName() != null
                    ? user.getFirstName().replaceAll("[^a-zA-Z0-9]", "") : "User";
            String fileName = safeFirstName + "_GodswillMatrimony.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}