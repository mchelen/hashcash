/*
 * Written by Jean-Luc Cooke <jlcooke@certainkey.com>
 * Implements HashCash v1
 * 
 * The goal of this JavaScript is for Web-based email sites to leverage HashCash techolgoies
 *
 * ChangeLog:
 *  - 2007-12-31 Added fix from Marco Abiuso
 */

/* if set to true, JavaScript SHA1 implementation will be used only.
 *  Otherwise, the code below will ry to use the applet, and fall-back to the JavaScript
 *  code if applet cannot be loaded
 */
var hashcashv1_jsonly = true;

/*
 * Creates a hashcash token with DP of "bits" zero bits
 * in a seperate thread, function "callback" will be called with the desired token:
 *  function myCompleteCallback(tokenResult) {
 *     ... do stuff with tokenResult ...
 *  }
 *  function myUpdateCallback(percentCompleteEstimate) {
 *     ... do stuff with percentCompleteEstimate ...
 *     ... value may be greater then 100, or less.  But
 *         statistically we expect it to complete once we reach 100 ...
 *  }
 *  // Asynchonous operation!
 *  hashcashv1_create('myCallback', 'jdoe@isp.com', 16);
 */
function hashcashv1_create(updateCallback, completeCallback, str, extn, bits) {
  setTimeout("hashcashv1_create_hidden('"+ updateCallback+"','"+completeCallback+"','"+str+"','"+ extn +"','"+ bits +"');", 100);
}

function hashcashv1_create_hidden(updateCallback, completeCallback, str, extn, bits) {
  /* try using the applet (MUCH faster) */
  if (!hashcashv1_jsonly) {
    if (hashcashv1_appptr == null) {
      hashcashv1_jsonly = true;
    } else {
//      alert(completeCallback +"('"+ hashcashv1_appptr.create_loop +"{}"+ str+","+extn+","+bits+")" +"');");
      setTimeout("hashcashv1_updateAppletStatus('"+ updateCallback +"');", 100);
      eval(completeCallback +"('"+ hashcashv1_appptr.create_loop(""+str,""+extn,""+bits) +"');");
      return;
    }
  }

  // fall back on JavaScript... :(
  eval(completeCallback +"('"+ hashcashv1_create_loop(updateCallback, str,extn,bits) +"');");
}

function hashcashv1_updateAppletStatus(fcn) {
  var status = hashcashv1_appptr.getStatus();
  var stop;

  if (status < 0) {
    stop = true;
    status = -status;
  }

  eval(fcn +"('"+ status +"');");

  if (!stop)
    setTimeout("hashcashv1_updateAppletStatus('"+ fcn +"');", 100);
}

function hashcashv1_create_loop(updateCallback, str, extn, bits) {
  d = new Date();
  var year = d.getYear() % 100;
  var month = d.getMonth()+1;
  var day = d.getDate();
  var hour = d.getHours();
  var min = d.getMinutes();
  var random = (new Number(Math.floor(Math.random() * 4294967296))).toString(16) +
               (new Number(Math.floor(Math.random() * 4294967296))).toString(16);
  var fiddle = new Number(1);
  var input;
  var k = 0, tot = 1 << bits;
  var result = -1;
  var mask = 0xffffffff;

  mask = mask << (32-bits);
  year = Math.floor(year/10) +""+ (year%10);
  month = Math.floor(month/10) +""+ (month%10);
  day = Math.floor(day/10) +""+ (day%10);
  hour = Math.floor(hour/10) +""+ (hour%10);
  min = Math.floor(min/10) +""+ (min%10);
  var searchString = "1:"+ bits +":"+ year + month + day + hour + min +":"+ str +":"+ extn +":"+ random +":";
//  var searchString = "1:0408050947:"+ str +":"+ extn +":0000000000000000:";
  while ((result & mask) != 0) {
    input = searchString + fiddle.toString(16);
    result = calcSHA1(input);
    fiddle += 1; k++;
    if ((k & 0xff) == 0  &&  updateCallback != null)
      eval(updateCallback +"("+ (k/tot) +");");
//      alert("res ="+ hex(result) +" "+ hex(mask&result) );
  }
//  eval("updateCallback(100);");

  return input;
}

var hex_chr = "0123456789abcdef";
function hex(num) {
  var str = "";
  for (var j=7; j>=0; j--)
    str += hex_chr.charAt((num >> (j * 4)) & 0x0F);
  return str;
}

function hashcashv1_check(token) {
  var res = calcSHA1(token);
  var bits = 0;
  for (var i=31; 0<=i; i--) {
    if (((res>>>i) & 1) != 0)
      break;
    bits++;
  }
  alert("There are "+ bits +" zero bits in SHA1('"+ token +"') res="+hex(res));
}
function hashcashv1_selftest() {
  var res = calcSHA1("abc");
  // a9993e36 4706816a ba3e2571 7850c26c 9cd0d89d
  if (hex(res).indexOf("a9993e36") != 0) {
    alert("HashCashv1 Self test FAILED! res="+ hex(res) +" correct="+ hex(0xa9993e36));
  }
}


hashcashv1_appptr = null;
function calcSHA1(str) {
  if (hashcashv1_appptr == null  &&  !hashcashv1_jsonly) {
    if ((hashcashv1_appptr=document.applets["hashcashv1_applet"]) == null || hashcashv1_appptr.test == null) {
      alert("Could not load the HashCash-version-1 applet!!!  Falling back to JavaScript-only mode.");
      hashcashv1_jsonly = true;
    }
  }

  if (hashcashv1_jsonly)
    return calcSHA1_js(str);

  return hashcashv1_appptr.calcSHA1(str);
}


/*
 * Convert a string to a sequence of 16-word blocks, stored as an array.
 * Append padding bits and the length, as described in the SHA1 standard.
 */
function str2blks_SHA1(str) {
  var nblk = ((str.length + 8) >> 6) + 1;
  var blks = new Array(nblk*16);
  for (var i=0; i<nblk*16; i++) blks[i] = 0;
  for (var i=0; i<str.length; i++)
    blks[i >> 2] |= str.charCodeAt(i) << (24 - (i % 4) * 8);
  blks[i >> 2] |= 0x80 << (24 - (i % 4) * 8);
  blks[nblk * 16 - 1] = str.length * 8;
  return blks;
}

function calcSHA1_js(str) {
  var x = str2blks_SHA1(str);
  var w = new Array(80);
  var t;
  var a = 0x67452301;
  var b = 0xEFCDAB89;
  var c = 0x98BADCFE;
  var d = 0x10325476;
  var e = 0xC3D2E1F0;


  for(var i=0; i<x.length; i+=16) {
    var olda = a;
    var oldb = b;
    var oldc = c;
    var oldd = d;
    var olde = e;

    for(var j=0; j<80; j++) {
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

//  return hex(a) + hex(b) + hex(c) + hex(d) + hex(e);
  return a;
}
