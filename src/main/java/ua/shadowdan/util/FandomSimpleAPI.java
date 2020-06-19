package ua.shadowdan.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
public class FandomSimpleAPI {

    private static final String USERID_QUERY = "https://community.fandom.com/api.php?action=query&format=json&list=users&ususers=%s";
    private static final String WALL_MESSAGES_LOCATION_QUERY = "https://community.fandom.com/wiki/Message_Wall:%s";

    private FandomSimpleAPI() { }

    public static long getUserId(String username) {
        String response = HttpUtil.doGetRequest(String.format(USERID_QUERY, username));

        if (response == null) {
            return -1;
        }

        JSONObject responseJson = new JSONObject(response);
        JSONArray users = responseJson.getJSONObject("query").getJSONArray("users");
        if (users.length() == 0) {
            return -1;
        }
        return users.getJSONObject(0).getLong("userid");
    }

    @NonNull
    @SneakyThrows
    public static List<FandomWallMessage> getAllMessagesOnWall(String username) {
        List<FandomWallMessage> list = new ArrayList<>();
        Document document = Jsoup.connect(String.format(WALL_MESSAGES_LOCATION_QUERY, username)).get();
        Elements allMessagesOnWall = document.select("li.SpeechBubble.message.message-main.message-1");
        for (Element messageBubble : allMessagesOnWall) {
            Element messageTitleElement = messageBubble.getElementsByClass("msg-title").first();
            Element editedByElement = messageBubble.getElementsByClass("edited-by").first();
            Element messageBodyElement = messageBubble.getElementsByClass("msg-body").first();

            String messageTitle = messageTitleElement.getElementsByTag("a").text();
            String editedBy = editedByElement.getElementsByTag("a").first().attr("href");
            String messageBody = messageBodyElement.text();

            list.add(new FandomWallMessage(messageTitle, editedBy, messageBody));
        }
        return list;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FandomWallMessage {
        private final String title;
        private final String editedBy;
        private final String body;

        @Override
        public String toString() {
            return "FandomWallMessage{" +
                    "title='" + title + '\'' +
                    ", editedBy='" + editedBy + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FandomWallMessage that = (FandomWallMessage) o;
            return Objects.equals(title, that.title) &&
                    Objects.equals(editedBy, that.editedBy) &&
                    Objects.equals(body, that.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, editedBy, body);
        }
    }

}
