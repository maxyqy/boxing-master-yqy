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

package com.bilibili.boxing.model.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The base entity for media.
 *
 * @author ChenSL
 */
public abstract class BaseMedia implements Parcelable {
    protected enum TYPE {
        IMAGE, VIDEO
    }

    protected boolean mIsSelected;
    protected long mDateAdd;
    //========yqy changed 2017-09-29===================

    protected String mPath;
    protected String mId;
    protected String mSize;

    public BaseMedia() {
    }

    public BaseMedia(String id, String path) {
        mId = id;
        mPath = path;
    }

    public abstract TYPE getType();

    public String getId() {
        return mId;
    }

    public long getSize() {
        try {
            long result = Long.valueOf(mSize);
            return result > 0 ? result : 0;
        }catch (NumberFormatException size) {
            return 0;
        }
    }

    public void setId(String id) {
        mId = id;
    }

    public void setSize(String size) {
        mSize = size;
    }

    public String getPath(){
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    //========yqy changed 2017-09-29===================
    public boolean isSelected() {
        return mIsSelected;
    }
    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }
    public long getDateAdd() {
        return mDateAdd;
    }
    public void setDateAdd(long dateAdd){
        this.mDateAdd=dateAdd;
    }
    //========yqy changed 2017-09-29===================

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.mIsSelected ? (byte) 1 : (byte) 0);
        dest.writeLong(this.mDateAdd);
        //========yqy changed 2017-09-29===================
        dest.writeString(this.mPath);
        dest.writeString(this.mId);
        dest.writeString(this.mSize);
    }

    protected BaseMedia(Parcel in) {
        this.mIsSelected = in.readByte() != 0;
        this.mDateAdd = in.readLong();
        //========yqy changed 2017-09-29===================
        this.mPath = in.readString();
        this.mId = in.readString();
        this.mSize = in.readString();
    }

}