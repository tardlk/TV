package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.event.RefreshEvent;
import org.greenrobot.eventbus.EventBus;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfirmClearDialog extends BaseDialog {
    private final TextView tvYes;
    private final TextView tvNo;

    public ConfirmClearDialog(@NonNull @NotNull Context context, String type) {
        this(context, () -> {
            if ("Collect".equals(type)) RoomDataManger.deleteVodCollectAll();
            else if ("History".equals(type)) RoomDataManger.deleteVodRecordAll();
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
        });
    }

    public ConfirmClearDialog(@NonNull Context context, Runnable confirmed) {
        super(context);
        setContentView(R.layout.dialog_confirm);
        setCanceledOnTouchOutside(true);
        tvYes = findViewById(R.id.btnConfirm);
        tvNo = findViewById(R.id.btnCancel);

        tvYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmed.run();
                ConfirmClearDialog.this.dismiss();
            }
        });
        tvNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmClearDialog.this.dismiss();
            }
        });
    }

}