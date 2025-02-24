package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun2ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "faaf0020003986277107274106200041f297001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0104086717598a0000000206051f64011f00ff0d014441544120414c41524d202afaaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200028f804001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0106051f64011f00faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200041f297001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0104086717598a0000000206051f64011f00ff0d014441544120414c41524d202afaaf"));

    }

}
