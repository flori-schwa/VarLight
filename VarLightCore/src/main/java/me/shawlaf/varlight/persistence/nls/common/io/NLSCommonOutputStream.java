package me.shawlaf.varlight.persistence.nls.common.io;

import me.shawlaf.varlight.persistence.nls.common.NLSConstants;
import me.shawlaf.varlight.persistence.nls.common.NibbleArray;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NLSCommonOutputStream extends DataOutputStream {

    public NLSCommonOutputStream(OutputStream out) {
        super(out);
    }

    public void writeNLSMagic() throws IOException {
        writeInt(NLSConstants.NLS_MAGIC);
    }

    public void writeNibbleArray(NibbleArray array) throws IOException {
        write(array.toByteArray());
    }
}
