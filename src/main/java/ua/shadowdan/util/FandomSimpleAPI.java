package ua.shadowdan.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
public class FandomSimpleAPI {

    private static final String WALL_MESSAGES_LOCATION_QUERY = "https://community.fandom.com/wiki/Message_Wall:%s";
    private static final String USER_ID_QUERY = "https://community.wikia.com/api.php?action=query&format=json&list=users&ususers=%s";

    /* USER INFO QUERY */
    private static final String USER_DETAILS_QUERY = "https://community.fandom.com/api/v1/User/Details/?ids=%s";
    private static final String BASIC_USER_INFO_QUERY = "https://community.wikia.com/api.php?action=query&format=json&list=users&usprop=blockinfo|gender|editcount|registration&ususers=%s";

    public static final Pattern NICK_PATTERN = Pattern.compile("https?://(?:[a-zA-Z]{2}\\.)?[a-zA-Z-]+\\.fandom\\.com(?:/[a-z]{2,3})?/wiki/(?:Message_Wall|Стена_обсуждения|Участник|User|User_talk):(?:%20)?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private FandomSimpleAPI() { }

    public static long getUserId(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format(USER_ID_QUERY, username)))
                .build();

        final JSONObject responseJson;
        try {
            String response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            responseJson = new JSONObject(response);
        } catch (IOException | InterruptedException ignored) {
            return -1;
        }

        JSONArray users = responseJson.getJSONObject("query").getJSONArray("users");
        if (users.length() == 0) {
            return -1;
        }
        return users.getJSONObject(0).getLong("userid");
    }

    /*public static long getUserId(String username) {
        List<FandomUserSimple> userInfo = getBasicUserInfo(username);
        return userInfo.size() > 0 ? userInfo.get(0).getUserID() : -1;
    }*/

    public static String getUserName(long userID) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format(USER_DETAILS_QUERY, userID)))
                .build();

        try {
            String response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return new JSONObject(response).getJSONArray("items").getJSONObject(0).getString("name");
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error while retrieving username", ex);
        }
    }

    @NonNull
    @SneakyThrows
    public static List<FandomWallMessage> getAllMessagesOnWall(String username, String accessToken) {
        List<FandomWallMessage> list = new ArrayList<>();
        Document document = Jsoup.connect(String.format(WALL_MESSAGES_LOCATION_QUERY, username))
                .cookie("access_token", accessToken)
                .get();
        Elements allMessagesOnWall = document.select("li.SpeechBubble.message.message-main.message-1");
        for (Element messageBubble : allMessagesOnWall) {
            Element messageTitleElement = messageBubble.getElementsByClass("msg-title").first();
            Element editedByElement = messageBubble.getElementsByClass("edited-by").first();
            Element messageBodyElement = messageBubble.getElementsByClass("msg-body").first();

            int messageId = Integer.parseInt(messageBubble.attr("data-id"));
            String messageTitle = messageTitleElement.getElementsByTag("a").text();
            String editedBy = editedByElement.getElementsByTag("a").first().attr("href");
            String messageBody = messageBodyElement.text();

            list.add(new FandomWallMessage(messageId, messageTitle, editedBy, messageBody));
        }
        return list;
    }

    @NotNull
    public static List<FandomUserSimple> getBasicUserInfo(String... names) {
        final String formattedUrl = String.format(BASIC_USER_INFO_QUERY, String.join("|", names))
                .replace("|", "%7C");
        HttpRequest basicInfoRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(formattedUrl))
                .build();

        final JSONArray users;
        try {
            String basicInfoResponse = HTTP_CLIENT.send(basicInfoRequest, HttpResponse.BodyHandlers.ofString()).body();
            users = new JSONObject(basicInfoResponse).getJSONObject("query").getJSONArray("users");
        } catch (IOException | InterruptedException ignored) {
            return Collections.emptyList();
        }

        StringJoiner ids = new StringJoiner(",");
        for (int o = 0; o < users.length(); o++) {
            long userId = users.getJSONObject(o).getLong("userid");
            ids.add(Long.toString(userId));
        }

        HttpRequest detailsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format(USER_DETAILS_QUERY, ids.toString())))
                .build();

        JSONArray usersDetails = new JSONArray();
        try {
            String detailsResponse = HTTP_CLIENT.send(detailsRequest, HttpResponse.BodyHandlers.ofString()).body();
            usersDetails = new JSONObject(detailsResponse).getJSONArray("items");
        } catch (IOException | InterruptedException ignored) { }

        List<FandomUserSimple> list = new ArrayList<>();

        for (int i = 0; i < users.length(); i++) {
            JSONObject jsonObject = users.getJSONObject(0);
            long userID = jsonObject.getLong("userid");
            String name = jsonObject.getString("name");
            int editCount = jsonObject.getInt("editcount");
            String registrationDate = jsonObject.getString("registration");
            String gender = jsonObject.getString("gender");
            String avatarUrl = null;

            for (int j = 0; j < usersDetails.length(); j++) {
                JSONObject detailsJsonObject = usersDetails.getJSONObject(j);
                if (detailsJsonObject.getLong("user_id") == userID) {
                    avatarUrl = detailsJsonObject.getString("avatar");
                    break;
                }
            }

            list.add(new FandomUserSimple(userID, name, editCount, registrationDate, avatarUrl, gender));
        }
        return list;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FandomWallMessage {
        private final int id;
        private final String title;
        private final String editedBy;
        private final String body;

        @Override
        public String toString() {
            return "FandomWallMessage{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", editedBy='" + editedBy + '\'' +
                    ", body='" + body + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FandomWallMessage that = (FandomWallMessage) o;
            return id == that.id &&
                    Objects.equals(title, that.title) &&
                    Objects.equals(editedBy, that.editedBy) &&
                    Objects.equals(body, that.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, title, editedBy, body);
        }
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FandomUserSimple {
        private final long userID;
        private final String name;
        private final int editCount;
        private final String registrationDate;
        private final String avatarUrl;
        private final String gender;

        @Override
        public String toString() {
            return "FandomUserSimple{" +
                    "userID=" + userID +
                    ", name='" + name + '\'' +
                    ", editCount=" + editCount +
                    ", registrationDate='" + registrationDate + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    ", gender='" + gender + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FandomUserSimple that = (FandomUserSimple) o;
            return userID == that.userID &&
                    editCount == that.editCount &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(registrationDate, that.registrationDate) &&
                    Objects.equals(avatarUrl, that.avatarUrl) &&
                    Objects.equals(gender, that.gender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userID, name, editCount, registrationDate, avatarUrl, gender);
        }
    }

}
