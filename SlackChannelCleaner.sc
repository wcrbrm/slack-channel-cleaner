import $ivy.`org.scalaj::scalaj-http:2.4.1`, scalaj.http.{ Http, HttpResponse }
import $ivy.`com.lihaoyi::ujson:0.6.6`, ujson._

val channel = if (sys.env.contains("SLACK_CHANNEL_ID")) sys.env("SLACK_CHANNEL_ID") else ""	
val token = if (sys.env.contains("SLACK_TOKEN")) sys.env("SLACK_TOKEN") else ""	

def deleteMessage(ts: String) = {
  val payload = Js.Obj("channel" -> channel, "ts" -> ts, "as_user" -> true).toString
  val response: HttpResponse[String] = Http("https://slack.com/api/chat.delete")
    .header("Content-Type", "application/json; charset=utf-8")
    .header("Authorization", s"Bearer ${token}")
    .postData(payload)
    .asString
  println(response.body);
  Thread.sleep(300)    
}

def listBotMessages: List[String] = {
  val response: HttpResponse[String] = Http("https://slack.com/api/channels.history")
     .param("channel", channel)
     .param("oldest", "143000000")
     .param("token", token)
     .param("count","1000")
     .asString
  val json = ujson.read(response.body)
  val result: List[String] = json("messages").arr.map(message => {
     val ts:String = message("ts").str
     val fields = message.obj.keys.toSet
     if (fields.contains("type") && fields.contains("subtype")) {
       val typ:String = message("type").str
       val subtype:String = message("subtype").str
       if (typ == "message" && subtype == "bot_message") ts else ""
     } else ""
  }).toList.filter(!_.isEmpty)
  result
}

if (channel.isEmpty) println("Missing SLACK_CHANNEL_ID")
if (token.isEmpty) println("Missing SLACK_TOKEN")

if (!channel.isEmpty && !token.isEmpty) {
  listBotMessages.foreach(ts => {
    println(ts)
    deleteMessage(ts)
  })
}
