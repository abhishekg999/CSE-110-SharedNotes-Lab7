package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: - getNote (maybe getNoteAsync)
    // TODO: - putNote (don't need putNotAsync, probably)
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }

    @WorkerThread
    public Note getNote(String title) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedTitle = title.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedTitle)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body();
            if (responseBody != null) {
                var body = responseBody.string();
                Log.i("GET NOTE", body);
                if (response.code() == 200) {
                    return Note.fromJSON(body);
                } else {
                    Log.i("GET NOTE", body);
                    return null;
                }
            } else {
                Log.e("GET NOTE", "Empty response body");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Note getNoteAsync(String title) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> getNote(title));

        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @WorkerThread
    public boolean updateNote(Note note) {
        String title = note.title;

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("PUT", RequestBody.create(note.toJSON(), MediaType.get("application/json; charset=utf-8")))
                .build();

        try (var response = client.newCall(request).execute()) {
            int statusCode = response.code();
            if (statusCode == 200) {
                return true;
            } else {
                Log.e("UPDATE NOTE", "Update failed: " + response.body().string());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @AnyThread
    public void updateNoteAsync(Note note) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> updateNote(note));
    }
}
