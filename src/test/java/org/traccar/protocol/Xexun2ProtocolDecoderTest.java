package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Xexun2ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new Xexun2ProtocolDecoder(null));

        verifyNull(decoder, binary(
                "faaf0020018e8620920603427420005be66d001f67b2e6e444eebda3c6161eff41ad999a061f000500000301003125201d1b01050867b2e6e4000000000605136405b400080a67b2e6e4000000000000ff1b0153544f502031333a33363a303420706f7765723a343233343736faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200028f804001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0106051f64011f00faaf"));
//
//        verifyPosition(decoder, binary(
//                "faaf0020003986277107274106200041f297001f6717598a450ba750463229a54252cccd1230000000000101000e332f2f2f0104086717598a0000000206051f64011f00ff0d014441544120414c41524d202afaaf"));

    }

}
