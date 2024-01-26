package org.apache.fineract.infrastructure.eclipselink;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import java.security.SecureRandom;
import java.util.Vector;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sessions.Session;

public class NanoIdSequence extends Sequence implements SessionCustomizer {

    private static final char[] BASE58_ALPHABET = { '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    private static final int LENGTH = 21;

    public NanoIdSequence(String name) {
        super(name);
    }

    public NanoIdSequence() {}

    @Override
    public Object getGeneratedValue(Accessor accessor, AbstractSession writeSession, String seqName) {
        return NanoIdUtils.randomNanoId(new SecureRandom(), BASE58_ALPHABET, LENGTH);
    }

    @Override
    public Vector getGeneratedVector(Accessor accessor, AbstractSession writeSession, String seqName, int size) {
        return null;
    }

    @Override
    public void onConnect() {}

    @Override
    public void onDisconnect() {}

    @Override
    public boolean shouldAcquireValueAfterInsert() {
        return false;
    }

    @Override
    public boolean shouldUseTransaction() {
        return false;
    }

    @Override
    public boolean shouldUsePreallocation() {
        return false;
    }

    public void customize(Session session) {
        NanoIdSequence sequence = new NanoIdSequence("nanoIdSequence");
        session.getLogin().addSequence(sequence);
    }

}
