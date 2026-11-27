package com.kavix.app
import java.util.Locale
class Rule(val l:String,val p:Int,val c:String,pat:String,val hard:Boolean=false){val re=Regex(pat,RegexOption.IGNORE_CASE)}
class Hit(val l:String,val p:Int)
class Verdict(val level:Int,val score:Int,val hits:List<Hit>,val top:String)
object Engine{
 private val R=listOf(
  Rule("Asks you to share OTP/PIN/CVV",45,"creds","""(share|send|enter|tell|give|batao|bhejo|dena|dijiye|daalo|dalo)\W+(\w+\W+){0,3}(otp|pin|cvv|password)\b"""),
  Rule("UPI PIN used to receive money (PIN only sends money)",60,"upi","""(enter|share|put|daalo|dalo|dijiye)\W+(\w+\W+){0,3}upi\s*pin|upi\s*pin.{0,40}(receive|credit|refund|cashback)|(receive|paisa|cashback|refund).{0,40}upi\s*pin|collect request""",true),
  Rule("Fake KYC / account-block threat",30,"kyc","""\bkyc\b|केवाईसी|(pan|aadhaar|aadhar)\W+(card\W+)?(update|link|expire|verify)|(account|khata|खाता)\W+.{0,25}(block|freez|suspend|band|deactivat|बंद)"""),
  Rule("Utility disconnection threat",35,"kyc","""(bijli|electricity|power|बिजली).{0,30}(cut|kat|disconnect|कट)|(connection|supply).{0,20}(disconnect|cut)"""),
  Rule("Artificial urgency",15,"urg","""(urgent|immediately|turant|abhi|aaj raat|last chance|final notice|expires? (today|soon)|within \d+\s*(hours?|hrs|minutes?|mins)|24 hours|तुरंत|अभी)"""),
  Rule("Lottery / prize bait",30,"prize","""(lottery|lucky draw|\bkbc\b|you (have )?won|jackpot|congratulations|inaam|lakh rupees|crore|jeeta hai|इनाम)"""),
  Rule("Upfront fee demanded",30,"prize","""(registration|processing|training|kit|refundable|security|clearance|release)\s*(fee|charge|deposit|amount)|(pay|bharo|jama)\W+.{0,25}(fee|charges?)"""),
  Rule("Work-from-home / task job bait",30,"job","""(work from home|part[- ]?time job|ghar baithe|earn\s*(rs\.?|₹)?\s*\d|daily (income|payout)|like (and|&) earn|data entry job|घर बैठे)"""),
  Rule("Guaranteed-return / crypto / trading bait",30,"crypto","""(guaranteed|risk[- ]?free|double your|100% (profit|return)|crypto|bitcoin|usdt|trading (group|signal)|forex)"""),
  Rule("'Digital arrest' / police-agency impersonation",40,"arrest","""(digital arrest|\bcbi\b|narcotics|money laundering|arrest warrant|enforcement directorate|mumbai police|customs.{0,30}parcel|parcel.{0,40}(drugs|narcotics|illegal))"""),
  Rule("Tech-support / remote-access scam",35,"tech","""(your (device|computer|account|phone) (is|has been) (hacked|compromised|infected)|virus|microsoft support|apple support|anydesk|teamviewer|quicksupport|rustdesk)"""),
  Rule("Sextortion / blackmail wording",40,"sext","""(your (video|photos?|nudes?)|webcam|i (have )?hacked|leak.{0,30}(contacts|friends|family)|pay.{0,20}bitcoin)"""),
  Rule("Fake delivery / challan / fine",25,"deliv","""(parcel|package|courier|delivery|e-?challan|traffic fine).{0,50}(fail|held|pending|pay|fee|address|reschedule)"""),
  Rule("Refund / cashback / reward claim",25,"upi","""(refund|cashback|reward points?|redeem).{0,40}(click|link|claim|expire)|claim.{0,20}(refund|cashback|reward)"""),
  Rule("Family-emergency money request",30,"fam","""(papa|mummy|beta|bhaiya|didi|mom|dad|bro).{0,60}(accident|hospital|arrested|emergency).{0,60}(send|transfer|pay|paise|paisa)|new number.{0,20}(save|urgent)"""),
  Rule("Threat of legal action / penalty",15,"urg","""(legal action|case will be filed|penalty|blocked permanently|jail|\bfir\b|कार्रवाई)""")
 )
 private val BR=listOf("sbi" to listOf("sbi.co.in","onlinesbi.sbi","sbi.bank.in","sbicard.com"),"hdfc" to listOf("hdfcbank.com","hdfcbank.net","hdfc.com"),"icici" to listOf("icicibank.com","icicidirect.com"),"axis" to listOf("axisbank.com"),"kotak" to listOf("kotak.com"),"pnb" to listOf("pnbindia.in"),"paytm" to listOf("paytm.com"),"phonepe" to listOf("phonepe.com"),"amazon" to listOf("amazon.in","amazon.com"),"flipkart" to listOf("flipkart.com"),"irctc" to listOf("irctc.co.in"),"incometax" to listOf("incometax.gov.in"),"uidai" to listOf("uidai.gov.in"),"jio" to listOf("jio.com"),"airtel" to listOf("airtel.in"))
 private val SHORT=setOf("bit.ly","tinyurl.com","t.co","goo.gl","cutt.ly","rb.gy","is.gd","shorturl.at","tiny.cc","ow.ly","s.id","rebrand.ly")
 private val BAD=Regex("""\.(xyz|top|click|link|online|site|shop|vip|cc|icu|buzz|live|work|rest)$""")
 private val IP=Regex("""\d+\.\d+\.\d+\.\d+""")
 private val URL=Regex("""(https?://[^\s<>"']+|www\.[^\s<>"']+|\b[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:com|in|xyz|top|click|link|online|site|shop|vip|cc|icu|buzz|live|ly|gl|me|app)\b(?:/\S*)?)""",RegexOption.IGNORE_CASE)
 val ADV=mapOf(
  "upi" to "Don't enter your UPI PIN. A PIN only sends money, never receives it. Decline and block.",
  "kyc" to "Don't click. Banks never ask KYC via links. Open your bank's own app or call the number on your card.",
  "creds" to "Never share OTP, PIN, CVV or password. No real bank employee asks for them.",
  "prize" to "You can't win a contest you never entered. Never pay a fee to claim a prize.",
  "job" to "Real jobs don't charge you to start. Don't pay registration or kit fees.",
  "crypto" to "'Guaranteed returns' is the biggest red flag. Don't transfer money or install trading apps.",
  "arrest" to "Hang up. There is no 'digital arrest' in India. Tell family and call 1930.",
  "tech" to "Don't install remote-access apps like AnyDesk. Real support never cold-calls you.",
  "sext" to "Don't pay or reply. Screenshot, block, and report on cybercrime.gov.in.",
  "deliv" to "Check the parcel or fine on the official app or site yourself. Don't pay via a link.",
  "fam" to "Call the person back on their saved number before sending any money.",
  "apk" to "Do NOT install this file. If you did: turn off internet, uninstall, change passwords, call your bank and 1930.",
  "gen" to "Be careful. Don't click links or open attachments. Verify through an official channel.")
 private fun host(u:String)=u.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').lowercase(Locale.ROOT)
 private fun regd(h:String):String{val p=h.split(".");val t=p.takeLast(2).joinToString(".");return if(Regex("^(co|org|gov|nic|net|ac)\\.in$").matches(t)&&p.size>2)p.takeLast(3).joinToString(".") else t}
 private fun ed(a:String,b:String):Int{val m=Array(a.length+1){IntArray(b.length+1)};for(i in 0..a.length)m[i][0]=i;for(j in 0..b.length)m[0][j]=j
  for(i in 1..a.length)for(j in 1..b.length)m[i][j]=minOf(m[i-1][j]+1,m[i][j-1]+1,m[i-1][j-1]+if(a[i-1]==b[j-1])0 else 1);return m[a.length][b.length]}
 private fun brand(h:String):String?{
  if(BR.any{(_,d)->d.any{h==it||h.endsWith(".$it")}})return null
  val toks=h.split('-','.');val lab=regd(h).substringBefore('.')
  for((k,_) in BR){if(toks.any{it==k||it.startsWith(k)||(k.length>=5&&it.contains(k))}||(k.length>=5&&ed(lab,k)<=2))return k}
  return null}
 fun analyze(text:String,sender:String,src:String):Verdict{
  val l=text.lowercase(Locale.ROOT);val hits=ArrayList<Hit>();val cat=HashMap<String,Int>();var hard=false
  fun add(s:String,p:Int,c:String,h:Boolean=false){hits.add(Hit(s,p));cat[c]=(cat[c]?:0)+p;if(h)hard=true}
  for(r in R)if(r.re.containsMatchIn(l))add(r.l,r.p,r.c,r.hard)
  val urls=URL.findAll(text).map{it.value.trimEnd('.',',',';',')')}.toList()
  for(u in urls){val h=host(u)
   if(h in SHORT)add("Shortened link hides the real site ($h)",20,"link")
   if(u.contains(".apk",true))add("Link downloads an .apk app",60,"apk",true)
   if(BAD.containsMatchIn(h))add("Risky domain ending: $h",20,"link")
   if(IP.matches(h))add("Link uses a raw IP address",25,"link")
   brand(h)?.let{add("Lookalike of '$it': $h is not official",40,"kyc")}}
  if(!hits.any{it.l.contains("apk")}&&l.contains(".apk"))add("Mentions an .apk file",50,"apk",true)
  if(src=="SMS"&&sender.isNotBlank()){
   val s=sender.replace(Regex("[\\s-]"),"")
   val hdr=Regex("^[A-Za-z]{2}-[A-Za-z0-9]{3,6}(-[A-Za-z])?$").matches(sender.trim())
   val mob=Regex("^(\\+91|91|0)?[6-9]\\d{9}$").matches(s)
   val claims=BR.any{l.contains(it.first)}||(cat["kyc"]?:0)>0||(cat["creds"]?:0)>0
   if(mob&&claims)add("Claims to be a bank/brand but sent from a personal mobile number",25,"kyc")
   else if(hdr&&hits.isEmpty())add("Sent from a registered sender header",-10,"ok")}
  if(urls.isEmpty()&&Regex("\\b\\d{4,8}\\b").containsMatchIn(l)&&Regex("otp|one time password").containsMatchIn(l)&&Regex("do not share|don't share|don’t share|never share|kisi ko na").containsMatchIn(l))add("Looks like a genuine OTP notice. Never share the code",-40,"ok")
  if(urls.isEmpty()&&Regex("(debited|credited).{0,60}(a/c|account|xx\\d+)").containsMatchIn(l)&&hits.none{it.p>=30})add("Plain transaction alert with no link",-20,"ok")
  var score=hits.sumOf{it.p}.coerceIn(0,100)
  if(hard)score=maxOf(score,85)
  val level=if(hard||score>=60)2 else if(score>=25)1 else 0
  val top=cat.filterKeys{it !in setOf("ok","urg","link")}.maxByOrNull{it.value}?.key?:(if(cat.containsKey("link"))"kyc" else "gen")
  return Verdict(level,score,hits,top)}
}
