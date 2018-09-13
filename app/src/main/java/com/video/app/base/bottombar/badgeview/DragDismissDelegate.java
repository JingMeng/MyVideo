/**
 * Copyright 2015 bingoogolapple
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.video.app.base.bottombar.badgeview;

/**
 * @ClassName: DragDismissDelegate
 * @author: 张京伟
 * @date: 2017/1/3 10:29
 * @Description: 拖动消失的代理
 * @version: 1.0
 */
public interface DragDismissDelegate {

    /**
     * 拖动大于BGABadgeViewHelper.mMoveHiddenThreshold后抬起手指徽章消失的回调方法
     *
     * @param badgeable
     */
    void onDismiss(Badgeable badgeable);
}