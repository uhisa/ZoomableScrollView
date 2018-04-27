# ZoomableScrollView
Any Views and Layouts can zoom and scroll vertically and horizontally in ZoomableScrollView

![ZoomableScrollView](ZoomableScrollView.gif)

### Usage

add the ZoomableScrollView to xml and set the View or Layout that you want to zoom and scroll as child of the ZoomableScrollView.

```xml
<com.uhisa.zoomablescrollview.ZoomableScrollView
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:background="@color/verticalBackground"
    app:gravity_center="false">

    <!-- your layout -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Hello world"
            android:gravity="center"/>
    </LinearLayout>
</com.uhisa.zoomablescrollview.ZoomableScrollView>
```

License
--------

    Copyright 2018 uhisa

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
