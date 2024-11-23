#!/usr/bin/env python3

import sys
import os
import xml.etree.ElementTree
import urllib
import urllib.request as urllib2
import json
import socket
import time
import threading
import re

messages = {
    'gps103' : 'imei:123456789012345,help me,1201011201,,F,120100.000,A,6000.0000,N,13000.0000,E,0.00,;',
    'tk103' : '(123456789012BP05123456789012345120101A6000.0000N13000.0000E000.0120200000.0000000000L000946BB)',
    'gl100' : '+RESP:GTSOS,123456789012345,0,0,0,1,0.0,0,0.0,1,130.000000,60.000000,20120101120300,0460,0000,18d8,6141,00,11F0,0102120204\0',
    'gl200' : '+RESP:GTFRI,020102,123456789012345,,0,0,1,1,0.0,0,0.0,130.000000,60.000000,20120101120400,0460,0000,18d8,6141,00,,20120101120400,11F0$',
    't55' : '$PGID,123456789012345*0F\r\n$GPRMC,120500.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,*33\r\n',
    'xexun' : '111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,, imei:123456789012345,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576\n',
    'totem' : '$$B3123456789012345|AA$GPRMC,120700.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A*74|01.8|01.0|01.5|000000000000|20120403234603|14251914|00000000|0012D888|0000|0.0000|3674|940B\r\n',
    'suntech' : 'SA200STT;123456;042;20120101;12:11:00;16d41;-15.618767;-056.083214;000.011;000.00;11;1;41557;12.21;000000;1;3205\r',
    'h02' : '*HQ,123456789012345,V1,121300,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,ffffffff,000000,000000,000000,000000#',
    'jt600' : '(1234567890,P45,290322,132412,25.28217,S,57.54683,W,A,0,0,5,0,0000000000,0,0,9,0)',
    'v680' : '#123456789012345#1000#0#1000#AUT#1#66830FFB#13000.0000,E,6000.0000,N,001.41,259#010112#121600##',
    'pt502' : '$POS,123456,121700.000,A,6000.0000,N,13000.0000,E,0.0,0.0,010112,,,A/00000,00000/0/23895000//\r\n',
    'tr20' : '%%123456789012345,A,120101121800,N6000.0000E13000.0000,0,000,0,01034802,150,[Message]\r\n',
    'meitrack' : '$$d138,123456789012345,AAA,35,60.000000,130.000000,120101122000,A,7,18,0,0,0,49,3800,24965,510|10|0081|4F4F,0000,000D|0010|0012|0963|0000,,*BF\r\n',
    'megastek' : 'STX,102110830074542,$GPRMC,122400.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*64,F,LowBattery,imei:123456789012345,03,113.1,Battery=24%,,1,460,01,2531,647E;57\r\n',
    'gpsgate' : '$FRLIN,IMEI,123456789012345,*7B\r\n$GPRMC,122600.000,A,6000.00000,N,13000.00000,E,0.000,0.0,010112,,*0A\r\n',
    'tlt2h' : '#123456789012345#V500#0000#AUTO#1\r\n#$GPRMC,123000.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,D*70\r\n##\r\n',
    'taip' : '>REV481669045060+6000000-1300000000000012;ID=123456789012345<',
    'wondex' : '123456789012345,20120101123200,130.000000,60.000000,0,000,0,0,2\r\n',
    'ywt' : '%RP,123456789012345:0,120101123500,E130.000000,N60.000000,,0,0,4,0,00\r\n',
    'tk102' : '[!0000000081r(123456789012345,TK102-W998_01_V1.1.001_130219,255,001,255,001,0,100,100,0,internet,0000,0000,0,0,255,0,4,1,11,00)][=00000000836(ITV123600A6000.0000N13000.0000E000.00001011210010000)]',
    'wialon' : '#L#123456789012345;test\r\n#SD#010112;123900;6000.0000;N;13000.0000;E;0;0;0;4\r\n',
    'carscop' : '*040331141830UB05123456789012345010112A6000.0000N13000.0000E000.0124000000.0000000000L000000^',
    'manpower' : 'simei:123456789012345,,,tracker,51,24,1.73,120101124200,A,6000.0000,N,13000.0000,E,0.00,28B9,1DED,425,01,1x0x0*0x1*60x+2,en-us,;',
    'globalsat' : '$123456789012345,1,1,010112,124300,E13000.0000,N6000.0000,00000,0.0100,147,07,2.4!',
    'pt3000' : '%123456789012345,$GPRMC,124500.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A,+100000000000,N098d',
    'topflytech' : '(123456789012345BP00XG00b600000000L00074b54S00000000R0C0F0014000100f0120101124700A6000.0000N13000.0000E000.0000.00)',
    'laipac' : '$AVRMC,123456789012345,124800,a,6000.0000,N,13000.0000,E,0.00,0.00,010112,0,3.727,17,1,0,0*17\r\n',
    'gotop' : '#123456789012345,CMD-T,A,DATE:120101,TIME:125000,LAT:60.0000000N,LOT:130.0000000E,Speed:000.0,84-20,000#',
    'sanav' : 'imei:123456789012345rmc:$GPRMC,093604.354,A,4735.0862,N,01905.2146,E,0.00,0.00,171013,,*09,AUTO-4103mv',
    'easytrack' : '*ET,123456789012345,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123#',
    'gpsmarker' : '$GM200123456789012345T100511123300N55516789E03756123400000035230298#\r',
    'stl060' : '$1,123456789012345,D001,AP29AW0963,23/02/14,14:06:54,17248488N,078342226E,0.08,193.12,1,1,1,1,1,A#',
    'cartrack' : '$$123456????????&A9955&B102904.000,A,2233.0655,N,11404.9440,E,0.00,,030109,,*17|6.3|&C0100000100&D000024?>&E10000000##',
    'minifinder' : '!1,123456789012345;!A,01/01/12,12:15:00,60.000000,130.000000,0.0,25101,0;',
    'haicom' : '$GPRS123456789012345,T100001,150618,230031,5402267400332464,0004,2014,000001,,,1,00#V040*',
    'box' : 'H,BT,123456789012345,081028142432,F5813D19,6D6E6DC2\rL,081028142429,G,52.51084,-1.70849,0,170,0,1,0\r',
    'freedom' : 'IMEI,123456789012345,2014/05/22, 20:49:32, N, Lat:4725.9624, E, Lon:01912.5483, Spd:5.05\r\n',
    'telic' : '182012345699,010100001301,0,270613041652,166653,475341,3,0,355,6,2,1,231,8112432,23201,01,00,217,0,0,0,0,7\0',
    'trackbox' : 'a=connect&v=11&i=123456789012345\r\n183457.999,5126.0247N,00002.8686E,5.2,70.4,3,57.63,32.11,17.32,150507,05\r\n',
    'visiontek' : '$1,AP09BU9397,123456789012345,20,06,14,15,03,28,17267339N,078279407E,060.0,073,0550,11,0,1,0,0,1,1,26,A,0000000000#',
    'tr900' : '>123456,4,1,150626,131252,W05830.2978,S3137.2783,,00,348,18,00,003-000,0,3,11111011*3b!\r\n',
    'ardi01' : '123456789012345,20141010052719,24.4736042,56.8445807,110,289,40,7,5,78,-1\r\n',
    'xt013' : 'TK,123456789012345,150131090859,+53.267863,+5.767363,0,38,12,0,F,204,08,C94,336C,24,,4.09,1,,,,,,,,\r\n',
    'gosafe' : '*GS16,123456789012345,100356130215,,SYS:G79W;V1.06;V1.0.2,GPS:A;6;N24.802700;E46.616828;0;0;684;1.35,COT:60,ADC:4.31;0.10,DTT:20000;;0;0;0;1#',
    'xirgo' : '$$123456789012345,6001,2013/01/22,15:36:18,25.80907,-80.32531,7.1,19,165.2,11,0.8,11.1,17,1,1,3.9,2##',
    'mtx' : '#MTX,123456789012345,20101226,195550,41.6296399,002.3611174,000,035,000000.00,X,X,1111,000,0,0\r\n',
    'aquila' : '$$SRINI_1MS,123456,1,12.963515,77.533844,150925161628,A,27,0,8,0,68,0,0,0,0,0,0,0,0,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,*43\r\n',
    'flextrack' : '-1,LOGON,123456,8945000000\r-2,UNITSTAT,20060101,123442,1080424008,N0.00.0000,E0.00.0000,0,0,0,4129,-61,2,23866,0,999,A214,63,2EE2,3471676\r',
    'watch' : '[3G*1234567890*00FD*UD,160617,120759,V,18.917990,S,47.5450083,E,0.00,0.0,0.0,0,87,1,0,0,00000011,7,255,646,2,81,11552,140,81,10281,127,81,10602,127,81,11553,126,81,10284,121,81,11122,119,81,10662,119,2,NETGEAR20,44:94:fc:43:5b:78,-47,TP-LINK_7650,98:de:d0:46:76:50,-88,46.5]',
    'upro' : '*AI2001234567890,BA&A2003064913201201845107561627121016&B0100000000&C05>8=961&F0333&K023101002154A7#',
    'auro' : 'M0030T0000816398975I123456789012345E00001W*****110620150441000068DA#RD01DA240000000000+100408391+013756125100620152140102362238320034400\r\n',
    'disha' : '$A#A#123456789012345#A#053523#010216#2232.7733#N#08821.1940#E#002.75#038.1#09#00.8#1800#0#000#0000#9999#11.7#285.7#0001*\r\n',
    'arnavi' : '$AV,V3,123456,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E\r\n',
    'kenji' : '>C123456,M005004,O0000,I0002,D124057,A,S3137.2783,W05830.2978,T000.0,H254.3,Y240116,G06*17\r\n',
    'fox' : '<fox><gps id="123456" data="0,A,110316,131834,4448.8355,N,02028.2021,E,0,217,,1111111111111111 123 0 0 0 0 0 00000000 50020,50020 0"/></fox>',
    'gnx' : '$GNX_DIO,123456789012345,110,1,155627,121214,151244,121214,1,08.878321,N,076.643154,E,0,0,0,0,0,0,GNX01001,B1*\n\r',
    'arknav' : '123456789012345,05*850,000,L001,A,2459.3640,N,12125.2958,E,000.0,224.8,00.8,07:47:26 09-09-05,9.00,D3,0,C4,1,,,,\r',
    'supermate' : '2:123456789012345:1:*,00000000,XT,A,10031b,140b28,80ad4c72,81ba2d2c,06ab,238c,020204010000,12,0,0000,0003e6#',
    'appello' : 'FOLLOWIT,123456789012345,160211221959,-12.112660,-77.045258,1,0,6,116,F,716,17,4E85,050C,29,,4.22,,39,999/00/00,,,,,,46206,\r\n',
    'idpl' : '*ID1,123456789012345,210314,162752,A,1831.4412,N,07351.0983,E,0.04,213.84,9,25,A,1,4.20,0,1,01,1,0,0,A01,L,EA01#\r\n',
    'hunterpro' : '>1234<$GPRMC,170559.000,A,0328.3045,N,07630.0735,W,0.73,266.16,200816,,,A77, s000078015180",0MD\r',
    'raveon' : '$PRAVE,1234,0001,3308.9051,-11713.1164,000000,1,10,168,31,13.3,3,-83,0,0,,1003.4*66\r\n',
    'cradlepoint' : '123456789012,000000,4337.174385,N,11612.338373,W,0.0,,Verizon,,-71,-44,-11,,\r\n',
    'arknavx8' : '123456789012345,241111;1R,110509053244,A,2457.9141N,12126.3321E,220.0,315,10.0,00000000;',
    'autograde' : '(000000007322123456789012345170415A1001.1971N07618.1375E0.000145312128.59?A0024B0024C0000D0000E0000K0000L0000M0000N0000O0000)',
    'cguard' : 'IDRO:123456789012345\r\nNV:170409 031456:56.808553:60.595476:0:NAN:0\r\n',
    'fifotrack' : '$$105,123456789012345,AB,A00,,161007085534,A,54.738791,25.271918,0,350,151,0,17929,0000,0,,246|1|65|96DB,936|0*0B\r\n',
    'extremtrac' : '$GPRMC,123456789012345,050859.000,A,1404.8573,N,08710.9967,W,0.00,0,080117,0,,00C8,00218,99,,,,,,0.00\r\n',
    'trakmate' : '^TMSRT|123456789012345|12.59675|77.56789|123456|030414|1.03|1.01|#',
    'maestro' : '@123456789012345,601,UPV-02,0,13.4,10,0,0,16/11/04,17:21:14,0.352793,32.647927,0,0,0,0,99,0.000,0!\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
    'gt30' : '$$005D1234567890    9955102834.000,A,3802.8629,N,02349.7163,E,0.00,,060117,,*13|1.3|26225BD\r\n',
    'tmg' : '$nor,L,123456789012345,24062015,094459,4,2826.1956,N,07659.7690,E,67.5,2.5,167,0.82,15,22,airtel,31,4441,1,4.1,12.7,00000011,00000011,1111,0.0,0.0,21.3,SW00.01,#\r\n',
    'pretrace' : '(123456789012345U1110A1701201500102238.1700N11401.9324E000264000000000009001790000000,&P11A4,F1050^47)',
    'siwi' : '$SIWI,1234,1320,A,0,,1,1,0,0,876578,43,10,A,19.0123456,72.65347,45,0,055929,071107,22,5,1,0,3700,1210,0,2500,1230,321,0,1.1,4.0,1!\r\n',
    'starlink' : '$SLU123456,06,622,170329035057,01,170329035057,+3158.0018,+03446.6968,004.9,007,000099,1,1,0,0,0,0,0,0,,,14.176,03.826,,1,1,1,4*B0\r\n',
    'alematics' : '$T,50,592,123456789012345,20170515062915,20170515062915,25.035005,121.561555,0,31,89,3.7,5,1,0,0.000,12.752,1629,38,12752,4203,6\r\n',
    'vtfms' : '(123456789012345,00I76,00,000,,,,,A,133755,210617,10.57354,077.24912,SW,000,00598,00000,K,0017368,1,12.7,,,0.000,,,0,0,0,0,1,1,0,,)074',
    'esky': 'ET;0;123456789012345;R;6+190317162511+41.32536+19.83144+0.14+0+0x0+0+18460312+0+1233+192',
    'genx': '123456789012345,08/31/2017 17:24:13,45.47275,-73.65491,5,19,117,1.14,147,ON,1462,0,6,N,0,0.000,-95.0,-1.0,0,0.0000,0.0000,0.000,0,0.00,0.00,0.00,NA,U,UUU,0,-95.0,U\r\n',
    'dway': 'AA55,115,1234,1,171024,195059,28.0153,-82.4761,3, 1.0,319,1000,0000,00000,4244,0,0,0,D\r\n',
    'oko': '{123456789012345,090745,A,4944.302,N,02353.366,E,0.0,225,251120,7,0.27,F9,11.3,1}',
    'ivt401': '(TLN,123456789012345,250118,063827,+18.598098,+73.806518,0,79,0,1,1,5,1200,0,0.0,11.50,4.00,36,0,0,1.00,0,0,12702,202,0);',
    't57': '*T57#F1#1234567890#301117#000843#2234.1303#N#08826.1714#E#+0.242,+0.109,-0.789#0.000#6.20000#A2#4.2#',
    'm2c': '[#M2C,2020,P1.B1.H1.F1.R1,101,123456789012345,2,L,1,100,170704,074933,28.647556,77.192940,900,194,0.0,0,0,0,255,11942,0,0,0,0,0,0,0,0,30068,5051,0,0,1*8159\r\n]',
    'cautela': '20,123456789012,14,02,18,16.816667,96.166667,1325,S,*2E\r\n',
    'pt60': '@B#@|01|001|123456789012345|9425010450971470|1|45|20181127122717|32.701093|35.570938|1|@R#@',
    'telemax': '%061234560128\r\nY000007C6999999067374074649003C00A7018074666F60D66818051304321900000000C5\r\n',
    'svias': '[7061,3041,57,1234567890,710,40618,141342,-93155840,-371754060,0,20469,0,16,1,0,0,11323,100,9,,32,4695]',
    'eseal': '##S,eSeal,123456,256,3.0.6,Normal,34,2017-08-31,08:14:40,15,A,25.708828N 100.372870W,10,0,Close,0.71,0:0:3:0,3.8,-73,E##\r\n',
    'avema': '1234567890,20190522093835,121.645898,25.062268,0,0,0,0,3,0.0,1,0.02,11.48,0,0,19,4,466-5,65534,56589841,0.01\r\n',
    'milesmate': 'ApiString={A:123456789012345,B:09.8,C:00.0,D:083506,E:2838.5529N,F:07717.8049E,G:000.00,H:170918,I:G,J:00004100,K:0000000A,L:1234,M:126.86}\r\n',
    'smartsole': '#GTXRP=123456789012345,8,180514200051,34.041981,-118.255806,60,1,1,7,1.80,180514200051,4.16,11$',
    'its': '$,EPB,SEM,123456789012345,NM,14072020112020,A,28.359959,N,076.927566,E,260.93,0.1,0.0,G,NA00000000,N.A0000000,*',
    'xrb28': '*SCOR,OM,123456789012345,D0,0,012102.00,A,0608.00062,S,10659.70331,E,12,0.69,151118,30.3,M,A#\r\n',
    'c2stek': 'PA$123456789012345$D#220222#135059#0#+37.98995#+23.85141#0.00#69.2#0.0#0000#000#8#00#sz-w1001#B2600$AP',
    'mictrack': 'MT;6;123456789012345;R0;10+190109091803+22.63827+114.02922+2.14+69+2+3744+113',
    'plugin': '$$STATUS123456,20190528143943,28.254086,-25.860665,0,0,0,-1,2,78,11395,0,0,0#',
    'racedynamics': '$GPRMC,12,260819,100708,123456789012345,\r\n$GPRMC,15,04,H,#,100632,A,1255.5106,N,07738.2954,E,001,260819,0887,06,1,00011,%,0000000000000000,000,000,0,0,1,0713,0,416,0,255,000,0,000,3258,000,000,00,0000,000,00000,0,F3VF01\r\n',
    's168': 'S168#123456789012345#0f12#0077#LOCA:G;CELL:1,1cc,2,2795,1435,64;GDATA:A,12,160412154800,22.564025,113.242329,5.5,152,900;ALERT:0000;STATUS:89,98;WAY:0$',
    'dingtek': '800001011e0692001a00000000016e008027c40000112345678901234581',
    'portman': '$PTMLA,123456789012345,A,200612153351,N2543.0681W10009.2974,0,190,NA,C9830000,NA,108,8,2.66,16,GNA\r\n',
    'futureway': '410000003F2000020,IMEI:123456789012345,battery level:6,network type:7,CSQ:236F42410000009BA00004GPS:V,200902093333,0.000000N,0.000000E,0.000,0.000\r\nWIFI:3,1|90-67-1C-F7-21-6C|52&2|80-89-17-C6-79-A0|54&3|40-F4-20-EF-DD-2A|58\r\nLBS:460,0,46475066,69\r\n6A42',
    'net': '@L03612345678901234512271020161807037078881037233751000000010F850036980A4000!',
    'mobilogix': '[2020-10-25 20:45:09,T9,1,V1.2.3,123456789012,59,10.50,701,-25.236860,-45.708530,0,314]',
    'swiftech': '@@123456789012345,,0,102040,1023.9670,N,07606.8160,E,2.26,151220,A,0127,1,1,03962,00000,#',
    'ennfu': 'Ennfu:123456789012345,041504.00,A,3154.86654,N,11849.08737,E,0.053,,080121,20,3.72,21.4,V0.01$',
    'startek': '&&o125,123456789012345,000,0,,210702235150,A,27.263505,153.037061,11,1.2,0,0,31,5125,505|1|7032|8C89802,20,0000002D,00,00,01E2|019DF0\r\n',
    'hoopo': '{ "deviceId": "123456789012345", "assetName": "123456789012345", "assetType": "test", "eventData": { "latitude": 31.97498, "longitude": 34.80802, "locationName": "", "accuracyLevel": "High", "eventType": "Arrival", "batteryLevel": 100, "receiveTime": "2021-09-20T18:52:32Z" }, "eventTime": "2021-09-20T08:52:02Z", "serverReportTime": "0001-01-01T00:00:00Z" }',
    'techtocruz': '$$A120,123456789012345,211005105836,A,FLEX,KCB 947C,000.0,0,-1.38047,S,36.93951,E,1648.4,243.140,21,28,12.1,3.7,0,1,0,0,0,*F6',
    'flexapi': '${"topic":"v1/123456789012345/motion/info","payload":{"motion.ts":1641885877,"motion.ax":0.006344,"motion.ay":0.289384,"motion.az":-0.939156,"motion.gx":0.420000,"motion.gy":0.420000,"motion.gz":-0.280000}}xx\r\n',
    'jido': '*123456789012345,03,130517,160435,1820.5845,N,07833.2478,E,1,58#',
    'armoli': '[M123456789012345210122125205N38.735641E035.4727751E003340000000C00000E9E07FF:106AG505283H60E];',
    'teratrack': '{"MDeviceID":"022043756090","DiviceType":"1","DataType":"1","DataLength":"69","DateTime":"2022-03-09 10:56:01","Latitude":"-6.846451","Longitude":"39.316324","LongitudeState":"1","LatitudeState":"0","Speed":"90","Mileage":"0","FenceAlarm":"0","AreaAlarmID":"0","LockCutOff":"0","SealTampered":"0","MessageAck":"1","LockRope":"1","LockStatus":"1","LockOpen":"0","PasswordError":"0","CardNo":"60000644","IllegalCard":"0","LowPower":"0","UnCoverBack":"0","CoverStatus":"1","LockStuck":"0","Power":"79","GSM":"16","IMEI":"123456789012345","Index":"20","Slave":[]}',
    'envotech': '$80SLM,02,F,123456,130410155921,431750216,000040,0000,,00000000,\'13041015592110476673N10111459E001281*2A#',
    'bstpl': 'BSTPL$1,123456789012345,V,200722,045113,00.000000,0,00.00000,0,0,0,000,00,0,17,1,1,0,0,00.01,0,04.19,15B_190821,8991000907387031196F,12.27#',
}

baseUrl = 'http://localhost:8082'
user = { 'email' : 'admin', 'password' : 'admin' }

debug = '-v' in sys.argv

def load_ports():
    ports = {}
    dir = os.path.dirname(os.path.abspath(__file__))
    with open(dir + '/../src/main/java/org/traccar/config/PortConfigSuffix.java', 'r') as file:
        content = file.read()
    pattern = re.compile(r'PORTS\.put\("([^"]+)",\s*(\d+)\);')
    matches = pattern.findall(content)
    ports = {protocol: int(port) for protocol, port in matches}
    if debug:
        print('\nports: {ports!r}\n')
    return ports

def login():
    request = urllib2.Request(baseUrl + '/api/session')
    response = urllib2.urlopen(request, urllib.parse.urlencode(user).encode())
    if debug:
        print(f'\nlogin: {json.load(response)!r}\n')
    return response.headers.get('Set-Cookie')

def remove_devices(cookie):
    request = urllib2.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    response = urllib2.urlopen(request)
    data = json.load(response)
    if debug:
        print(f'\ndevices: {data!r}\n')
    for device in data:
        request = urllib2.Request(baseUrl + '/api/devices/' + str(device['id']))
        request.add_header('Cookie', cookie)
        request.get_method = lambda: 'DELETE'
        response = urllib2.urlopen(request)

def add_device(cookie, unique_id):
    request = urllib2.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    device = { 'name' : unique_id, 'uniqueId' : unique_id }
    response = urllib2.urlopen(request, json.dumps(device).encode())
    data = json.load(response)
    return data['id']

def send_message(port, message):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', port))
    s.send(message.encode('ascii'))
    time.sleep(0.5)
    s.close()
    time.sleep(0.5)

def get_protocols(cookie, device_id):
    params = { 'deviceId' : device_id, 'from' : '2000-01-01T00:00:00.000Z', 'to' : '2050-01-01T00:00:00.000Z' }
    request = urllib2.Request(baseUrl + '/api/positions?' + urllib.parse.urlencode(params))
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    request.add_header('Accept', 'application/json')
    response = urllib2.urlopen(request)
    protocols = []
    for position in json.load(response):
        protocols.append(position['protocol'])
    return protocols

if __name__ == "__main__":
    ports = load_ports()

    cookie = login()
    remove_devices(cookie)

    devices = {
        '123456789012345' : add_device(cookie, '123456789012345'),
        '123456789012' : add_device(cookie, '123456789012'),
        '1234567890' : add_device(cookie, '1234567890'),
        '123456' : add_device(cookie, '123456'),
        '1234' : add_device(cookie, '1234')
    }

    all = set(ports.keys())
    protocols = set(messages.keys())

    print(f'Total: {len(all)}')
    print(f'Missing: {len(all - protocols)}')
    print(f'Covered: {len(protocols)}')

    for protocol in messages:
        send_message(ports[protocol], messages[protocol])

    for device in devices:
        protocols -= set(get_protocols(cookie, devices[device]))

    print(f'Success: {len(messages) - len(protocols)}')
    print(f'Failed:{len(protocols)}')

    if protocols:
        print(f'\nFailed: {list(protocols)!r}')
