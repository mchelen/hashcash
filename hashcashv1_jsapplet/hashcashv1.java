
import java.applet.Applet;
import java.util.*;

/*
 * Written by Jean-Luc Cooke <jlcooke@certainkey.com>
 * Implements HashCash v1
 * 
 * The goal of this JavaScript is for Web-based email sites to leverage HashCash techolgoies
 */

public class hashcashv1 extends Applet {

  private static Random rnd = new Random();
  public static String test = "test";

  /* this is required for an applet to work. */
  public void init() {
  }

/*
 * Convert a 32-bit number to a hex string with ms-byte first
 */
private static final byte[] hex_chr = "0123456789abcdef".getBytes();
private static final String hex(int num) {
  String str = "";
  for (int j=7; j>=0; j--)
    str += hex_chr[(num >> (j * 4)) & 0x0f];
  return str;
}

private int hashCount, hashCountExp;
public final double getStatus() {
  return (double) hashCount / hashCountExp;
}

public final String create_loop(String str, String extn, String bitsStr) {
try {
  int bits = Integer.parseInt(bitsStr);

  Calendar calendar = new GregorianCalendar();
  Date trialTime = new Date();
  calendar.setTime(trialTime);
  int min = calendar.get(Calendar.MINUTE);
  int hour = calendar.get(Calendar.HOUR);
  int day = calendar.get(Calendar.DAY_OF_MONTH);
  int month = calendar.get(Calendar.MONTH)+1;
  int year = calendar.get(Calendar.YEAR) % 100;
  byte[] hexstep = new byte[256];
  hexstep['0'] = '1';
  hexstep['1'] = '2';
  hexstep['2'] = '3';
  hexstep['3'] = '4';
  hexstep['4'] = '5';
  hexstep['5'] = '6';
  hexstep['6'] = '7';
  hexstep['7'] = '8';
  hexstep['8'] = '9';
  hexstep['9'] = 'a';
  hexstep['a'] = 'b';
  hexstep['b'] = 'c';
  hexstep['c'] = 'd';
  hexstep['d'] = 'e';
  hexstep['e'] = 'f';
  hexstep['f'] = '0';
  byte[] searchArr = ("1:"+
                     bits +":"+
                     (year/10) +""+ (year%10) +
                     (month/10) +""+ (month%10) +
                     (day/10) +""+ (day%10) +
                     (hour/10) +""+ (hour%10) +
                     (min/10) +""+ (min%10) +":"+
                     str +":"+
                     extn +":"+
                     Integer.toHexString(rnd.nextInt()) + Integer.toHexString(rnd.nextInt()) +":").getBytes();
  byte[] fiddle = {'0','0','0','0', '0','0','0','0'};
  byte input[] = new byte[searchArr.length + 8];
  for (int i=0; i<searchArr.length; i++)
    input[i] = searchArr[i];

  int result[] = {-1,0,0,0,0};
  int mask = 0xffffffff <<  (32-bits);

  hashCount = 0;
  hashCountExp = 1 << bits;

  while ((result[0] & mask) != 0) {
    if ((fiddle[7]=hexstep[fiddle[7]]) == '0')
    if ((fiddle[6]=hexstep[fiddle[6]]) == '0')
    if ((fiddle[5]=hexstep[fiddle[5]]) == '0')
    if ((fiddle[4]=hexstep[fiddle[4]]) == '0')
    if ((fiddle[3]=hexstep[fiddle[3]]) == '0')
    if ((fiddle[2]=hexstep[fiddle[2]]) == '0')
    if ((fiddle[1]=hexstep[fiddle[1]]) == '0')
      fiddle[0] = hexstep[fiddle[0]];
    input[searchArr.length  ] = fiddle[0];
    input[searchArr.length+1] = fiddle[1];
    input[searchArr.length+2] = fiddle[2];
    input[searchArr.length+3] = fiddle[3];
    input[searchArr.length+4] = fiddle[4];
    input[searchArr.length+5] = fiddle[5];
    input[searchArr.length+6] = fiddle[6];
    input[searchArr.length+7] = fiddle[7];
    result = calcSHA1_internal(input);
    hashCount++;
  }

  System.out.println("hashCount = "+ hashCount +"/"+ hashCountExp +" token="+ (new String(input)) +" hash={"+ result[0] +","+ result[1] +","+ result[2] +","+ result[3] +","+ result[4] +" mask="+ mask);
  hashCount = -hashCount;
  return new String(input);
} catch (Exception e) {
  e.printStackTrace();
  return "error: "+ e;
}
}


/*
 * Convert a string to a sequence of 16-word blocks, stored as an array.
 * Append padding bits and the length, as described in the SHA1 standard.
 */
private static final int[] str2blks_SHA1(byte strB[]) {
  int i,
      nblk = ((strB.length + 8) >> 6) + 1;
  int blks[] = new int[nblk*16];
  for (i=0; i<nblk*16; i++)
    blks[i] = 0;
  for (i=0; i<strB.length; i++)
    blks[i >> 2] |= strB[i] << (24 - (i % 4) * 8);
  blks[i >> 2] |= 0x80 << (24 - (i % 4) * 8);
  blks[nblk * 16 - 1] = strB.length * 8;
  return blks;
}

public final int calcSHA1(String str) {
  if (str == null) {
    System.out.println("hashcashv1.calcSHA1(str): str==NULL");
    return 0xffffffff;
  }
  int[] res = calcSHA1_internal(str.getBytes());
  System.out.println("res[0] = "+ Integer.toHexString(res[0]) +" "+ res[0]);
  return res[0];
}

private static final int[] calcSHA1_internal(byte str[]) {
  int x[] = str2blks_SHA1(str);
  int w[] = new int[80];
  int t, olda, oldb, oldc, oldd, olde;
  int a = 0x67452301,
      b = 0xEFCDAB89,
      c = 0x98BADCFE,
      d = 0x10325476,
      e = 0xC3D2E1F0;

  for(int i=0; i<x.length; i+=16) {
    olda = a;
    oldb = b;
    oldc = c;
    oldd = d;
    olde = e;

    for(int j=0; j<80; j++) {
      if(j < 16) w[j] = x[i + j];
      else { t = w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16]; w[j] = (t<<1) | (t>>>31); }
    }

    e = ((a<<5)|(a>>>27)) + ((b&c)|((~b)&d)) + e + w[ 0] + 0x5A827999; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|((~a)&c)) + d + w[ 1] + 0x5A827999; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|((~e)&b)) + c + w[ 2] + 0x5A827999; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|((~d)&a)) + b + w[ 3] + 0x5A827999; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|((~c)&e)) + a + w[ 4] + 0x5A827999; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|((~b)&d)) + e + w[ 5] + 0x5A827999; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|((~a)&c)) + d + w[ 6] + 0x5A827999; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|((~e)&b)) + c + w[ 7] + 0x5A827999; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|((~d)&a)) + b + w[ 8] + 0x5A827999; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|((~c)&e)) + a + w[ 9] + 0x5A827999; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|((~b)&d)) + e + w[10] + 0x5A827999; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|((~a)&c)) + d + w[11] + 0x5A827999; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|((~e)&b)) + c + w[12] + 0x5A827999; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|((~d)&a)) + b + w[13] + 0x5A827999; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|((~c)&e)) + a + w[14] + 0x5A827999; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|((~b)&d)) + e + w[15] + 0x5A827999; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|((~a)&c)) + d + w[16] + 0x5A827999; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|((~e)&b)) + c + w[17] + 0x5A827999; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|((~d)&a)) + b + w[18] + 0x5A827999; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|((~c)&e)) + a + w[19] + 0x5A827999; c=(c<<30)|(c>>>2);

    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[20] + 0x6ED9EBA1; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[21] + 0x6ED9EBA1; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[22] + 0x6ED9EBA1; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[23] + 0x6ED9EBA1; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[24] + 0x6ED9EBA1; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[25] + 0x6ED9EBA1; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[26] + 0x6ED9EBA1; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[27] + 0x6ED9EBA1; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[28] + 0x6ED9EBA1; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[29] + 0x6ED9EBA1; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[30] + 0x6ED9EBA1; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[31] + 0x6ED9EBA1; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[32] + 0x6ED9EBA1; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[33] + 0x6ED9EBA1; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[34] + 0x6ED9EBA1; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[35] + 0x6ED9EBA1; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[36] + 0x6ED9EBA1; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[37] + 0x6ED9EBA1; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[38] + 0x6ED9EBA1; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[39] + 0x6ED9EBA1; c=(c<<30)|(c>>>2);

    e = ((a<<5)|(a>>>27)) + ((b&c)|(b&d)|(c&d)) + e + w[40] + 0x8F1BBCDC; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|(a&c)|(b&c)) + d + w[41] + 0x8F1BBCDC; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|(e&b)|(a&b)) + c + w[42] + 0x8F1BBCDC; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|(d&a)|(e&a)) + b + w[43] + 0x8F1BBCDC; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|(c&e)|(d&e)) + a + w[44] + 0x8F1BBCDC; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|(b&d)|(c&d)) + e + w[45] + 0x8F1BBCDC; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|(a&c)|(b&c)) + d + w[46] + 0x8F1BBCDC; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|(e&b)|(a&b)) + c + w[47] + 0x8F1BBCDC; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|(d&a)|(e&a)) + b + w[48] + 0x8F1BBCDC; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|(c&e)|(d&e)) + a + w[49] + 0x8F1BBCDC; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|(b&d)|(c&d)) + e + w[50] + 0x8F1BBCDC; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|(a&c)|(b&c)) + d + w[51] + 0x8F1BBCDC; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|(e&b)|(a&b)) + c + w[52] + 0x8F1BBCDC; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|(d&a)|(e&a)) + b + w[53] + 0x8F1BBCDC; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|(c&e)|(d&e)) + a + w[54] + 0x8F1BBCDC; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + ((b&c)|(b&d)|(c&d)) + e + w[55] + 0x8F1BBCDC; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + ((a&b)|(a&c)|(b&c)) + d + w[56] + 0x8F1BBCDC; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + ((e&a)|(e&b)|(a&b)) + c + w[57] + 0x8F1BBCDC; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + ((d&e)|(d&a)|(e&a)) + b + w[58] + 0x8F1BBCDC; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + ((c&d)|(c&e)|(d&e)) + a + w[59] + 0x8F1BBCDC; c=(c<<30)|(c>>>2);

    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[60] + 0xCA62C1D6; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[61] + 0xCA62C1D6; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[62] + 0xCA62C1D6; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[63] + 0xCA62C1D6; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[64] + 0xCA62C1D6; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[65] + 0xCA62C1D6; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[66] + 0xCA62C1D6; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[67] + 0xCA62C1D6; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[68] + 0xCA62C1D6; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[69] + 0xCA62C1D6; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[70] + 0xCA62C1D6; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[71] + 0xCA62C1D6; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[72] + 0xCA62C1D6; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[73] + 0xCA62C1D6; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[74] + 0xCA62C1D6; c=(c<<30)|(c>>>2);
    e = ((a<<5)|(a>>>27)) + (b^c^d) + e + w[75] + 0xCA62C1D6; b=(b<<30)|(b>>>2);
    d = ((e<<5)|(e>>>27)) + (a^b^c) + d + w[76] + 0xCA62C1D6; a=(a<<30)|(a>>>2);
    c = ((d<<5)|(d>>>27)) + (e^a^b) + c + w[77] + 0xCA62C1D6; e=(e<<30)|(e>>>2);
    b = ((c<<5)|(c>>>27)) + (d^e^a) + b + w[78] + 0xCA62C1D6; d=(d<<30)|(d>>>2);
    a = ((b<<5)|(b>>>27)) + (c^d^e) + a + w[79] + 0xCA62C1D6; c=(c<<30)|(c>>>2);

    a += olda;
    b += oldb;
    c += oldc;
    d += oldd;
    e += olde;
  }

  int[] ret = {a,b,c,d,e};
  return ret;
}

public static void main(String arg[]) {
  final int LOOPS = 0x100000;
  hashcashv1 hc = new hashcashv1();

  int[] res = hc.calcSHA1_internal("abc".getBytes());
  int[] ans = {0xa9993e36, 0x4706816a, 0xba3e2571, 0x7850c26c, 0x9cd0d89d};

  System.out.println("calcSHA1('abc') = "+ hc.calcSHA1("abc"));

  System.out.print("SHA1('abc') = ");
  for (int i=0; i<res.length; i++)
    System.out.print(Integer.toHexString(res[i]));

  for (int i=0; i<ans.length; i++) {
    if (ans[i] != res[i]) {
      System.out.println(" FAILED!!!");
      return;
    }
  }
  System.out.println(" passed");

  System.out.println("create_loop: "+ hc.create_loop("jlcooke@jlcooke.ca","","10") );

  int resB[];
  long start = (new Date()).getTime();
  for (int i=0; i<LOOPS; i++)
    resB = hc.calcSHA1_internal("abc".getBytes());
  long end = (new Date()).getTime();

  System.out.println(""+ LOOPS +" SHA1's in "+ (end-start) +"msec = "+ (1000 * (double)LOOPS/(end-start)) +" SHA1/sec");
  
}

}
