/**
 * screenshot-protection.js
 * src/main/resources/static/js/screenshot-protection.js
 *
 * NO false positives. window.blur + visibilitychange intentionally removed.
 */
(function () {
    'use strict';

    var TOAST_MS          = 2500;
    var DEVTOOLS_PX       = 160;
    var DEVTOOLS_INTERVAL = 1000;
    var BLOCKED_KEYS      = { 'PrintScreen': true, 'F13': true };
    var toast      = null;
    var toastTimer = null;

    function showScreenshotWarning() {
        document.body.classList.add('ss-blur');
        if (toast) { toast.textContent = '⚠️  Screenshots are not allowed on this page.'; toast.style.display = 'block'; }
        try { if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText('').catch(function(){}); } catch(_){}
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            document.body.classList.remove('ss-blur');
            if (toast) toast.style.display = 'none';
        }, TOAST_MS);
    }

    function lockAllImages() {
        var sel = ['.profile-details-image img','.extra-photo-img','.extra-photo-logo','#lightboxImg'];
        document.querySelectorAll(sel.join(',')).forEach(function (img) {
            if (img.dataset.ssLocked) return;
            img.dataset.ssLocked = '1';
            img.addEventListener('contextmenu', function(e){ e.preventDefault(); e.stopPropagation(); return false; }, true);
            img.addEventListener('dragstart',   function(e){ e.preventDefault(); e.stopPropagation(); return false; }, true);
            img.addEventListener('mousedown',   function(e){ if(e.button===2) e.preventDefault(); }, true);
            img.removeAttribute('download');
        });
        ['.profile-details-image','.extra-photo-wrap','#photoLightbox'].forEach(function(s){
            document.querySelectorAll(s).forEach(function(el){
                if (el.dataset.ssLocked) return;
                el.dataset.ssLocked = '1';
                el.addEventListener('contextmenu', function(e){ e.preventDefault(); return false; }, true);
            });
        });
    }

    document.addEventListener('contextmenu', function(e){ e.preventDefault(); return false; }, true);
    document.addEventListener('copy', function(e){ e.preventDefault(); }, true);
    document.addEventListener('cut',  function(e){ e.preventDefault(); }, true);

    document.addEventListener('keydown', function(e){
        if (BLOCKED_KEYS[e.key]||BLOCKED_KEYS[e.code])                                                { e.preventDefault(); showScreenshotWarning(); return; }
        if (e.shiftKey&&(e.metaKey||e.ctrlKey)&&(e.key==='s'||e.key==='S'))                           { e.preventDefault(); showScreenshotWarning(); return; }
        if (e.metaKey&&e.shiftKey&&(e.key==='3'||e.key==='4'||e.key==='5'))                           { e.preventDefault(); showScreenshotWarning(); return; }
        if ((e.ctrlKey||e.metaKey)&&!e.shiftKey&&(e.key==='p'||e.key==='P'))                          { e.preventDefault(); showScreenshotWarning(); return; }
        if ((e.ctrlKey||e.metaKey)&&!e.shiftKey&&(e.key==='s'||e.key==='S'))                          { e.preventDefault(); showScreenshotWarning(); return; }
        if ((e.ctrlKey||e.metaKey)&&(e.key==='u'||e.key==='U'))                                       { e.preventDefault(); return; }
        if (e.key==='F12')                                                                             { e.preventDefault(); return; }
        if ((e.ctrlKey||e.metaKey)&&e.shiftKey&&(e.key==='i'||e.key==='I'))                           { e.preventDefault(); return; }
        if ((e.ctrlKey||e.metaKey)&&e.shiftKey&&(e.key==='c'||e.key==='C'))                           { e.preventDefault(); return; }
        if ((e.ctrlKey||e.metaKey)&&(e.key==='a'||e.key==='A'))                                       { e.preventDefault(); return; }
    }, true);

    document.addEventListener('keyup', function(e){
        if (BLOCKED_KEYS[e.key]||BLOCKED_KEYS[e.code]) showScreenshotWarning();
    }, true);

    window.print = function(){ showScreenshotWarning(); return false; };
    window.addEventListener('beforeprint', function(){ showScreenshotWarning(); document.body.classList.add('ss-blur'); });
    window.addEventListener('afterprint',  function(){ document.body.classList.remove('ss-blur'); });

    if (navigator.mediaDevices && navigator.mediaDevices.getDisplayMedia) {
        navigator.mediaDevices.getDisplayMedia = function(){ showScreenshotWarning(); return Promise.reject(new DOMException('Permission denied','NotAllowedError')); };
    }
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        var _g = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
        navigator.mediaDevices.getUserMedia = function(c){ if(c&&c.video){ showScreenshotWarning(); return Promise.reject(new DOMException('Permission denied','NotAllowedError')); } return _g(c); };
    }

    (function(){
        var open = false;
        setInterval(function(){
            var isOpen = (window.outerWidth-window.innerWidth>DEVTOOLS_PX)||(window.outerHeight-window.innerHeight>DEVTOOLS_PX);
            if (isOpen&&!open){ open=true; document.body.classList.add('devtools-open'); if(toast){toast.textContent='⚠️  Developer Tools detected. Access restricted.';toast.style.display='block';} }
            else if (!isOpen&&open){ open=false; document.body.classList.remove('devtools-open'); if(toast){toast.style.display='none';toast.textContent='⚠️  Screenshots are not allowed on this page.';} }
        }, DEVTOOLS_INTERVAL);
    })();

    document.addEventListener('DOMContentLoaded', function(){
        toast = document.getElementById('ss-warning');
        lockAllImages();
        var lbImg = document.getElementById('lightboxImg');
        if (lbImg) lbImg.addEventListener('load', lockAllImages);
    });

    window.ssLockImages = lockAllImages;
})();