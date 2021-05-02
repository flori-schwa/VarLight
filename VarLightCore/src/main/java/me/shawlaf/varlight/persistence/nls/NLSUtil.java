package me.shawlaf.varlight.persistence.nls;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.ChunkCoords;

@UtilityClass
public class NLSUtil {

    public static final int CURRENT_VERSION = 1;

    public static final int SIZEOF_INT16 = 2;
    public static final int SIZEOF_INT32 = 4;

    public static final int NLS_MAGIC = 0x4E_41_4C_53;

    public static final int SIZEOF_NLS_MAGIC = SIZEOF_INT32;

    public static ChunkCoords fromEncoded(int regionX, int regionZ, int encoded) {
        int x = 32 * regionX + (encoded & 0x1F);
        int z = 32 * regionZ + ((encoded >>> 5) & 0x1F);

        return new ChunkCoords(x, z);
    }

}
