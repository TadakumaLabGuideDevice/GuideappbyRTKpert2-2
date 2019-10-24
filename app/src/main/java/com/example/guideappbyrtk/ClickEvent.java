package com.example.guideappbyrtk;

import android.content.Intent;

interface ClickEvent {
    //VOICEボタンイベントここから------------------------------------------------------------------------
    //音声入力の結果を受け取るために onActivityResult を設置
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
