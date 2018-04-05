/*
 * Copyright (C) 2018 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.rxcursorloader;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Action;

import static com.doctoror.rxcursorloader.RxCursorLoader.TAG;
import static com.doctoror.rxcursorloader.RxCursorLoader.isDebugLoggingEnabled;

final class RxCursorLoaderFlowableFactory {

    @NonNull
    static Flowable<Cursor> create(
            @NonNull final ContentResolver resolver,
            @NonNull final RxCursorLoader.Query query,
            @NonNull final Scheduler scheduler,
            @NonNull final BackpressureStrategy backpressureStrategy) {
        //noinspection ConstantConditions
        if (resolver == null) {
            throw new NullPointerException("ContentResolver param must not be null");
        }
        //noinspection ConstantConditions
        if (query == null) {
            throw new NullPointerException("Query param must not be null");
        }

        final CursorLoaderOnSubscribe onSubscribe = new CursorLoaderOnSubscribe(
                resolver, query, scheduler);

        return Flowable
                .create(onSubscribe, backpressureStrategy)
                .subscribeOn(scheduler)
                .doFinally(new Action() {
                @Override
                public void run() {
                    onSubscribe.release();
                }
            });
    }

    private static final class CursorLoaderOnSubscribe
            implements FlowableOnSubscribe<Cursor> {

        private final Object mLock = new Object();

        @NonNull
        private final ContentResolver mContentResolver;

        @NonNull
        private final RxCursorLoader.Query mQuery;

        @NonNull
        private final Scheduler mScheduler;

        @NonNull
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        private FlowableEmitter<Cursor> mEmitter;

        private ContentObserver mResolverObserver;

        CursorLoaderOnSubscribe(
                @NonNull final ContentResolver resolver,
                @NonNull final RxCursorLoader.Query query,
                @NonNull final Scheduler scheduler) {
            mContentResolver = resolver;
            mQuery = query;
            mScheduler = scheduler;
        }

        @Override
        public void subscribe(final FlowableEmitter<Cursor> emitter) {
            synchronized (mLock) {
                mEmitter = emitter;
                mContentResolver.registerContentObserver(mQuery.contentUri, true,
                        getResolverObserver());
                reload();
            }
        }

        private void release() {
            synchronized (mLock) {
                if (mResolverObserver != null) {
                    mContentResolver.unregisterContentObserver(mResolverObserver);
                    mResolverObserver = null;
                }

                mEmitter = null;
            }
        }

        /**
         * Loads new {@link Cursor}.
         * <p>
         * This must be called from {@link #subscribe(FlowableEmitter)} thread
         */
        private void reload() {
            if (isDebugLoggingEnabled()) {
                Log.d(TAG, mQuery.toString());
            }

            final Cursor c = mContentResolver.query(
                mQuery.contentUri,
                mQuery.projection,
                mQuery.selection,
                mQuery.selectionArgs,
                mQuery.sortOrder);

            synchronized (mLock) {
                if (mEmitter != null && !mEmitter.isCancelled()) {
                    if (c != null) {
                        mEmitter.onNext(c);
                    } else {
                        mEmitter.onError(new QueryReturnedNullException());
                    }
                }
            }
        }

        /**
         * Creates the {@link ContentObserver} to observe {@link Cursor} changes.
         * It must be initialized from thread in which {@link #subscribe(FlowableEmitter)} is
         * called.
         *
         * @return the {@link ContentObserver} to observe {@link Cursor} changes.
         */
        @NonNull
        private ContentObserver getResolverObserver() {
            if (mResolverObserver == null) {
                mResolverObserver = new ContentObserver(mHandler) {

                    @Override
                    public void onChange(final boolean selfChange) {
                        super.onChange(selfChange);
                        mScheduler.scheduleDirect(mReloadRunnable);
                    }
                };
            }
            return mResolverObserver;
        }

        private final Runnable mReloadRunnable = new Runnable() {
            @Override
            public void run() {
                reload();
            }
        };
    }
}
