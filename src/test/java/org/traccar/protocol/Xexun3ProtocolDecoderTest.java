package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun3ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun3ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "faaf002042608620920603427420005bea65001f67bca08844eebd76c6161efd41480000091b0004000003010016211d191801050867bca0880000000006051256058000080a67bca088000000000000ff1b0153544f502032323a33383a333220706f7765723a333934353631faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200028f804001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0106051f64011f00faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200041f297001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0104086717598a0000000206051f64011f00ff0d014441544120414c41524d202afaaf"));

    }

}
