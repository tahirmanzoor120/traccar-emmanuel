package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun2ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "faaf002043aa865209073338445000a0d362001f67afd02d000000000000000000000000000000000000031000000000000000012f67afd02d06f4a4d6be8f30e97e4d8f43f360dcf4fbb8dbc8fcc460aaef3f141cbaa43b0e811e20b960aaef3f1421b9021167afd02d014e001400001f0d0499190423050867afd02d0000000006051019610000080a67afd02d001900000000ff1c0154494d45522030373a32313a353920706f7765723a333633383639faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200028f804001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0106051f64011f00faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200041f297001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0104086717598a0000000206051f64011f00ff0d014441544120414c41524d202afaaf"));

    }

}
