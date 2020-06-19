package ua.shadowdan.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * Created by SHADOWDAN_ on 04.06.2020 for project 'IscariBotV2'
 */
public class IOUtil {

    private IOUtil() { }

    @Nullable
    public static String readToString(@NotNull InputStream inputStream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                stringBuilder.append(inputLine);
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
