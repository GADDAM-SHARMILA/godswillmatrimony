package com.godswill.matrimony.service;

import com.godswill.matrimony.model.DatabaseSequence;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    private static final long START_VALUE = 1200; // First profile number
    private static final long STEP        = 7;    // 1200, 1207, 1214 ...

    /**
     * Generates the next profile number as a multiple-of-7 sequence.
     *
     * MongoDB 'seq' field = number of profiles created so far (1-based).
     * Profile number = START_VALUE + ((seq - 1) * STEP)
     *
     *   seq=1  → GWM-1200
     *   seq=2  → GWM-1207
     *   seq=3  → GWM-1214  ... etc.
     *
     * If the sequence document doesn't exist yet (first run), it checks
     * the highest existing profileNumber in the DB and resumes from there.
     */
    public long generateSequence(String seqName) {

        // ── Check if sequence document already exists ──
        DatabaseSequence existing = mongoOperations.findOne(
                Query.query(Criteria.where("_id").is(seqName)),
                DatabaseSequence.class
        );

        // ── First time: auto-detect from existing profiles ──
        if (existing == null || existing.getSeq() == 0) {
            long resumeIndex = detectResumeIndex(seqName);

            // Initialize the counter to resumeIndex so next increment = resumeIndex + 1
            DatabaseSequence init = new DatabaseSequence();
            init.setId(seqName);
            init.setSeq(resumeIndex);
            mongoOperations.save(init);
        }

        // ── Atomically increment and get new value ──
        DatabaseSequence counter = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(seqName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );

        long index = (counter == null || counter.getSeq() <= 0) ? 1 : counter.getSeq();

        return START_VALUE + ((index - 1) * STEP);
    }

    /**
     * Scans existing profiles to find the highest profile number already issued,
     * then calculates what the seq counter should be to resume correctly.
     *
     * e.g. if highest existing is GWM-1214 (index 3), returns 3
     * so next increment gives seq=4 → GWM-1221
     */
    private long detectResumeIndex(String seqName) {
        // Find all profile sequence documents of type GWM-*
        // We look at the raw sequence collection for the max issued number
        try {
            // Query the profiles collection for the highest profileNumber
            org.springframework.data.mongodb.core.query.Query profileQuery =
                    new org.springframework.data.mongodb.core.query.Query();
            profileQuery.with(org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "profileNumber"));
            profileQuery.limit(1);

            com.godswill.matrimony.model.Profile latest = mongoOperations.findOne(
                    profileQuery, com.godswill.matrimony.model.Profile.class);

            if (latest != null && latest.getProfileNumber() != null) {
                String pn = latest.getProfileNumber(); // e.g. "GWM-1214"
                // Extract the numeric part
                String numeric = pn.replaceAll("[^0-9]", "");
                if (!numeric.isEmpty()) {
                    long lastNumber = Long.parseLong(numeric);
                    // Reverse-calculate: index = ((lastNumber - START_VALUE) / STEP) + 1
                    long index = ((lastNumber - START_VALUE) / STEP) + 1;
                    System.out.println("✅ Sequence resumed from existing profile: "
                            + pn + " → next index will be " + (index + 1));
                    return index; // counter will be incremented to index+1 on next call
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not detect resume index: " + e.getMessage());
        }

        // No existing profiles — start fresh at 0 (first increment → seq=1 → GWM-1200)
        return 0;
    }
}