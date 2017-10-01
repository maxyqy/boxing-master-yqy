/*
 *  Copyright (C) 2017 Bilibili
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.bilibili.boxing.model.task.impl;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.bilibili.boxing.model.BoxingManager;
import com.bilibili.boxing.model.callback.IMediaTaskCallback;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.entity.BaseMedia;
import com.bilibili.boxing.model.entity.impl.ImageMedia;
import com.bilibili.boxing.model.entity.impl.VideoMedia;
import com.bilibili.boxing.model.task.IMediaTask;
import com.bilibili.boxing.utils.BoxingExecutor;
import com.bilibili.boxing.utils.BoxingLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A Task to load photos.
 *
 * @author ChenSL
 */
@WorkerThread
public class ImageTask implements IMediaTask<BaseMedia> {
//public class ImageTask implements IMediaTask<ImageMedia> {
//========yqy changed 2017-09-29===================
    private static final String SELECTION_IMAGE_MIME_TYPE = Images.Media.MIME_TYPE + "=? or " + Images.Media.MIME_TYPE + "=? or " + Images.Media.MIME_TYPE + "=? or " + Images.Media.MIME_TYPE + "=?";
    private static final String SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF = Images.Media.MIME_TYPE + "=? or " + Images.Media.MIME_TYPE + "=? or " + Images.Media.MIME_TYPE + "=?";
    private static final String SELECTION_ID = Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE + " )";
    private static final String SELECTION_ID_WITHOUT_GIF = Images.Media.BUCKET_ID + "=? and (" + SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF + " )";
    private static final String[] SELECTION_ARGS_IMAGE_MIME_TYPE = {"image/jpeg", "image/png", "image/jpg", "image/gif"};
    private static final String[] SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF = {"image/jpeg", "image/png", "image/jpg"};
    private BoxingConfig mPickerConfig;
    private Map<String, String> mThumbnailMap;

    private static final String MIME_TYPE = "mime_type";
    private static String[] MEDIA_COL = new String[]{
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
    };//========yqy changed 2017-09-29===================

    public ImageTask() {
        this.mThumbnailMap = new ArrayMap<>();
        this.mPickerConfig = BoxingManager.getInstance().getBoxingConfig();
    }

    @Override
    //public void load(@NonNull final ContentResolver cr, final int page, final String id,
    //                 @NonNull final IMediaTaskCallback<ImageMedia> callback) {
    public void load(@NonNull final ContentResolver cr, final int page, final String id,final boolean allVideo,
                     @NonNull final IMediaTaskCallback<BaseMedia> callback) {
        if(!allVideo) {
            buildThumbnail(cr);
        }
        buildAlbumList(cr, id, page, allVideo,callback);
    }
    //========yqy changed 2017-09-29===================

    private void buildThumbnail(ContentResolver cr) {
        String[] projection = {Images.Thumbnails.IMAGE_ID, Images.Thumbnails.DATA};
        queryThumbnails(cr, projection);
    }

    private void queryThumbnails(ContentResolver cr, String[] projection) {
        Cursor cur = null;
        try {
            cur = Images.Thumbnails.queryMiniThumbnails(cr, Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    Images.Thumbnails.MINI_KIND, projection);
            if (cur != null && cur.moveToFirst()) {
                do {
                    String imageId = cur.getString(cur.getColumnIndex(Images.Thumbnails.IMAGE_ID));
                    String imagePath = cur.getString(cur.getColumnIndex(Images.Thumbnails.DATA));
                    mThumbnailMap.put(imageId, imagePath);
                } while (cur.moveToNext() && !cur.isLast());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

   // private List<ImageMedia> buildAlbumList(ContentResolver cr, String bucketId, int page,
    //                                        @NonNull final IMediaTaskCallback<ImageMedia> callback) {
   // ========yqy changed 2017-09-29===================
   private List<BaseMedia> buildAlbumList(ContentResolver cr, String bucketId, int page,boolean allVideo,
                                          @NonNull final IMediaTaskCallback<BaseMedia> callback) {

        //List<ImageMedia> result = new ArrayList<>();
       List<BaseMedia> result = new ArrayList<>();
       //========yqy changed 2017-09-29===================

        String columns[] = getColumns();
        Cursor cursor = null;

       MergeCursor mergeCursor =null;
       //========yqy changed 2017-09-29===================

        try {
            if(allVideo){
                cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MEDIA_COL, null, null,
                        Images.Media.DATE_ADDED + " desc" + " LIMIT " + page * IMediaTask.PAGE_LIMIT + " , " + IMediaTask.PAGE_LIMIT);
                addVideoItem(result,cursor,callback);
            }else {
                boolean isDefaultAlbum = TextUtils.isEmpty(bucketId);
                boolean isNeedPaging = mPickerConfig == null || mPickerConfig.isNeedPaging();
                boolean isNeedGif = mPickerConfig != null && mPickerConfig.isNeedGif();
                int totalCount = getTotalCount(cr, bucketId, columns, isDefaultAlbum, isNeedGif);

                String imageMimeType = isNeedGif ? SELECTION_IMAGE_MIME_TYPE : SELECTION_IMAGE_MIME_TYPE_WITHOUT_GIF;
                String[] args = isNeedGif ? SELECTION_ARGS_IMAGE_MIME_TYPE : SELECTION_ARGS_IMAGE_MIME_TYPE_WITHOUT_GIF;
                String order = isNeedPaging ? Images.Media.DATE_ADDED + " desc" + " LIMIT "
                        + page * IMediaTask.PAGE_LIMIT + " , " + IMediaTask.PAGE_LIMIT : Images.Media.DATE_MODIFIED + " desc";
                String selectionId = isNeedGif ? SELECTION_ID : SELECTION_ID_WITHOUT_GIF;
                cursor = query(cr, bucketId, columns, isDefaultAlbum, isNeedGif, imageMimeType, args, order, selectionId);
                //========yqy changed 2017-09-29===================
                //addItem(totalCount, result, cursor, callback);
                if (mPickerConfig.isNeedVideo() && isDefaultAlbum) {
                    Cursor cursorVideo = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MEDIA_COL, null, null,
                            Images.Media.DATE_ADDED + " desc" + " LIMIT " + page * IMediaTask.PAGE_LIMIT + " , " + IMediaTask.PAGE_LIMIT);
                    if (cursorVideo != null) {
                        totalCount = totalCount + cursorVideo.getCount();
                        ArrayList<Cursor> cursorArrayList = new ArrayList<>();
                        cursorArrayList.add(cursor);
                        cursorArrayList.add(cursorVideo);
                        Cursor[] cursors = cursorArrayList.toArray(new Cursor[cursorArrayList.size()]);
                        mergeCursor = new MergeCursor(cursors);
                        addItem(totalCount, result, mergeCursor, callback);
                    } else {
                        addItem(totalCount, result, cursor, callback);
                    }
                } else {
                    addItem(totalCount, result, cursor, callback);
                }
            }

             //========yqy changed 2017-09-29===================
        } finally {

          /*  if (cursor != null) {
                cursor.close();
            }
            */
            if(mergeCursor!=null){
                mergeCursor.close();
            }else if (cursor != null) {
                cursor.close();
            }
            //========yqy changed 2017-09-29===================

        }
        return result;
    }

    //========yqy changed 2017-09-29===================
    //private void addItem(final int allCount, final List<ImageMedia> result, Cursor cursor, @NonNull final IMediaTaskCallback<ImageMedia> callback) {
    private void addItem(final int allCount, final List<BaseMedia> result, Cursor cursor, @NonNull final IMediaTaskCallback<BaseMedia> callback) {
         if (cursor != null && cursor.moveToFirst()) {
            do {
                String picPath = cursor.getString(cursor.getColumnIndex(Images.Media.DATA));
                if (callback.needFilter(picPath)) {
                    BoxingLog.d("path:" + picPath + " has been filter");
                } else {
                    String id = cursor.getString(cursor.getColumnIndex(Images.Media._ID));
                    String size = cursor.getString(cursor.getColumnIndex(Images.Media.SIZE));
                    String mimeType = cursor.getString(cursor.getColumnIndex(Images.Media.MIME_TYPE));
                    int width = 0;
                    int height = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        width = cursor.getInt(cursor.getColumnIndex(Images.Media.WIDTH));
                        height = cursor.getInt(cursor.getColumnIndex(Images.Media.HEIGHT));
                    }
                    ImageMedia imageItem = new ImageMedia.Builder(id, picPath).setThumbnailPath(mThumbnailMap.get(id))
                            .setSize(size).setMimeType(mimeType).setHeight(height).setWidth(width).build();
                    if (!result.contains(imageItem)) {
                        result.add(imageItem);
                    }
                }
            } while (!cursor.isLast() && cursor.moveToNext());
            postMedias(result, allCount, callback);
        } else {
            postMedias(result, 0, callback);
        }
        clear();
    }

    //========yqy changed 2017-09-29===================
    private void addItem(final int allCount, final List<BaseMedia> result, MergeCursor cursor, @NonNull final IMediaTaskCallback<BaseMedia> callback) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MIME_TYPE));
                if (mimeType.contains("video")) {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                    String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                    String date = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));
                    long dateAdd=cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                    VideoMedia video = new VideoMedia.Builder(id, data).setTitle(title).setDuration(duration)
                            .setSize(size).setDataTaken(date).setMimeType(mimeType).setDateAdd(dateAdd).build();
                    result.add(video);
                }else {
                    String picPath = cursor.getString(cursor.getColumnIndex(Images.Media.DATA));
                    if (callback.needFilter(picPath)) {
                        BoxingLog.d("path:" + picPath + " has been filter");
                    } else {
                        String id = cursor.getString(cursor.getColumnIndex(Images.Media._ID));
                        String size = cursor.getString(cursor.getColumnIndex(Images.Media.SIZE));
                        int width = 0;
                        int height = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            width = cursor.getInt(cursor.getColumnIndex(Images.Media.WIDTH));
                            height = cursor.getInt(cursor.getColumnIndex(Images.Media.HEIGHT));
                        }
                        long dateAdd=cursor.getLong(cursor.getColumnIndexOrThrow(Images.Media.DATE_ADDED));
                        ImageMedia imageItem = new ImageMedia.Builder(id, picPath).setThumbnailPath(mThumbnailMap.get(id))
                                .setSize(size).setMimeType(mimeType).setHeight(height).setWidth(width).setDateAdd(dateAdd).build();
                        if (!result.contains(imageItem)) {
                            result.add(imageItem);
                        }
                    }
                }
            } while (!cursor.isLast() && cursor.moveToNext());
            Collections.sort(result, new Comparator<BaseMedia>() {
                @Override
                public int compare(BaseMedia o1, BaseMedia o2) {
                    return o1.getDateAdd()>=o2.getDateAdd()? -1 : 1;//按照添加时间进行降序排序
                }
            });

            postMedias(result, allCount, callback);
        }else {
            postMedias(result, 0, callback);
        }
        clear();
    }
    //========yqy changed 2017-09-29===================

    private void addVideoItem(final List<BaseMedia> result,Cursor cursor, @NonNull final IMediaTaskCallback<BaseMedia> callback) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                    String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MIME_TYPE));
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                    String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                    String date = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));
                    long dateAdd=cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                    VideoMedia video = new VideoMedia.Builder(id, data).setTitle(title).setDuration(duration)
                            .setSize(size).setDataTaken(date).setMimeType(mimeType).setDateAdd(dateAdd).build();
                    result.add(video);
            } while (!cursor.isLast() && cursor.moveToNext());
            postMedias(result, cursor.getCount(), callback);
        }else {
            postMedias(result, 0, callback);
        }
        clear();
    }
    //========yqy changed 2017-09-29===================

    //private void postMedias(final List<ImageMedia> result, final int count, @NonNull final IMediaTaskCallback<ImageMedia> callback) {
    //========yqy changed 2017-09-29===================
      private void postMedias(final List<BaseMedia> result, final int count, @NonNull final IMediaTaskCallback<BaseMedia> callback) {
        BoxingExecutor.getInstance().runUI(new Runnable() {
            @Override
            public void run() {
                callback.postMedia(result, count);
            }
        });
    }

    private Cursor query(ContentResolver cr, String bucketId, String[] columns, boolean isDefaultAlbum,
                         boolean isNeedGif, String imageMimeType, String[] args, String order, String selectionId) {
        Cursor resultCursor;
        if (isDefaultAlbum) {
            resultCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, imageMimeType,
                    args, order);
        } else {
            if (isNeedGif) {
                resultCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, selectionId,
                        new String[]{bucketId, args[0], args[1], args[2], args[3]}, order);
            } else {
                resultCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, selectionId,
                        new String[]{bucketId, args[0], args[1], args[2]}, order);
            }
        }
        return resultCursor;
    }

    @NonNull
    private String[] getColumns() {
        String[] columns;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            columns = new String[]{Images.Media._ID, Images.Media.DATA, Images.Media.SIZE, Images.Media.MIME_TYPE, Images.Media.WIDTH, Images.Media.HEIGHT , Images.Media.DATE_ADDED};
        } else {
            columns = new String[]{Images.Media._ID, Images.Media.DATA, Images.Media.SIZE, Images.Media.MIME_TYPE, Images.Media.DATE_ADDED};
        }
        return columns;
    }

    private int getTotalCount(ContentResolver cr, String bucketId, String[] columns, boolean isDefaultAlbum, boolean isNeedGif) {
        Cursor allCursor = null;
        int result = 0;
        try {
            if (isDefaultAlbum) {
                allCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns,
                        SELECTION_IMAGE_MIME_TYPE, SELECTION_ARGS_IMAGE_MIME_TYPE,
                        Images.Media.DATE_MODIFIED + " desc");
            } else {
                if (isNeedGif) {
                    allCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, SELECTION_ID,
                            new String[]{bucketId, "image/jpeg", "image/png", "image/jpg", "image/gif"}, Images.Media.DATE_MODIFIED + " desc");
                } else {
                    allCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, columns, SELECTION_ID_WITHOUT_GIF,
                            new String[]{bucketId, "image/jpeg", "image/png", "image/jpg"}, Images.Media.DATE_MODIFIED + " desc");
                }
            }
            if (allCursor != null) {
                result = allCursor.getCount();
            }
        } finally {
            if (allCursor != null) {
                allCursor.close();
            }
        }
        return result;
    }

    private void clear() {
        if (mThumbnailMap != null) {
            mThumbnailMap.clear();
        }
    }

}
