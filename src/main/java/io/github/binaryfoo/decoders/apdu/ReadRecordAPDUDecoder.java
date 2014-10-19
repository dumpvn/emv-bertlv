package io.github.binaryfoo.decoders.apdu;

import io.github.binaryfoo.DecodedData;
import io.github.binaryfoo.decoders.DecodeSession;

public class ReadRecordAPDUDecoder implements CommandAPDUDecoder {
    @Override
    public APDUCommand getCommand() {
        return APDUCommand.ReadRecord;
    }

    @Override
    public DecodedData decode(String input, int startIndexInBytes, DecodeSession session) {
        String recordNumber = input.substring(4, 6);
        int referenceControlParameter = Integer.parseInt(input.substring(6, 8), 16);
        int sfi = (referenceControlParameter & 0x000000f8) >> 3;
        return DecodedData.primitive("C-APDU: Read Record", "number " + recordNumber + " SFI " + sfi, startIndexInBytes, startIndexInBytes + 5);
    }
}
