package com.thanhbinh84.rxjavawithretrofitsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testZip();
    }

    private void testZip() {
        Retrofit repo = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        Observable<JsonObject> userObservable = repo
                .create(GitHubUser.class)
                .getUser("thanhbinh84")
//                .getUser("fakeUser_fakeUser")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(Observable.just(new JsonObject())); // it will return empty json data when error occurs such as get fake users

        Observable<JsonArray> eventsObservable = repo
                .create(GitHubEvents.class)
                .listEvents("thanhbinh84")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());

        Observable<UserAndEvents> combined = Observable.zip(userObservable, eventsObservable, new Func2<JsonObject, JsonArray, UserAndEvents>() {
            @Override
            public UserAndEvents call(JsonObject jsonObject, JsonArray jsonElements) {
                return new UserAndEvents(jsonObject, jsonElements);
            }
        });

        combined.subscribe(new Subscriber<UserAndEvents>() {
            @Override
            public void onCompleted() {
                Log.wtf("TAG", "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.wtf("TAG", "onError");
                Log.wtf("TAG", e.toString());
            }

            @Override
            public void onNext(UserAndEvents o) {
                Log.wtf("TAG", "onNext");
                Log.wtf("Username", o.user.get("name").toString());
                Log.wtf("First event", o.events.get(0).getAsJsonObject().get("type").toString());
            }
        });
    }



    public interface GitHubUser {
        @GET("users/{user}")
        Observable<JsonObject> getUser(@Path("user") String user);
    }

    public interface GitHubEvents {
        @GET("users/{user}/events")
        Observable<JsonArray> listEvents(@Path("user") String user);
    }

    public class UserAndEvents {
        public UserAndEvents(JsonObject user, JsonArray events) {
            this.events = events;
            this.user = user;
        }

        public JsonArray events;
        public JsonObject user;
    }
}
