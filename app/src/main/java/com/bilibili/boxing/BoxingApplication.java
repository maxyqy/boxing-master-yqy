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

package com.bilibili.boxing;

import android.app.Application;

import com.bilibili.boxing.impl.BoxingGlideLoader;
import com.bilibili.boxing.impl.BoxingUcrop;
import com.bilibili.boxing.loader.IBoxingMediaLoader;

/**
 * aha, initial work.
 *
 * @author ChenSL
 */
public class BoxingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //IBoxingMediaLoader loader = new BoxingFrescoLoader(this); //Album 窗口列表滑动时，左侧某些行缩略图会不显示
        IBoxingMediaLoader loader = new BoxingGlideLoader();
        //========yqy changed 2017-09-29===================

        BoxingMediaLoader.getInstance().init(loader);
        BoxingCrop.getInstance().init(new BoxingUcrop());
    }
}
