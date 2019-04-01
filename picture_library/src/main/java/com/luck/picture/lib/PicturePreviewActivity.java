package com.luck.picture.lib;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.luck.picture.lib.adapter.BottomPreviewAdapter;
import com.luck.picture.lib.adapter.SimpleFragmentAdapter;
import com.luck.picture.lib.anim.OptAnimationLoader;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.immersive.ImmersiveManage;
import com.luck.picture.lib.observable.ImagesObservable;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.rxbus2.Subscribe;
import com.luck.picture.lib.rxbus2.ThreadMode;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.ToastManage;
import com.luck.picture.lib.tools.VoiceUtils;
import com.luck.picture.lib.widget.PreviewViewPager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */
public class PicturePreviewActivity extends PictureBaseActivity implements
        View.OnClickListener, Animation.AnimationListener, SimpleFragmentAdapter.OnCallBackActivity {
    private ImageView picture_left_back;
    private TextView tv_title, tv_ok;
    private PreviewViewPager viewPager;
    private int position;
    private LinearLayout ll_check;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private TextView check;
    private SimpleFragmentAdapter adapter;
    private Animation animation;
    private boolean refresh;
    private int index;
    private int screenWidth;
    private Handler mHandler;

    boolean is_bottom_preview;

    private RecyclerView recycleBottom;
    private BottomPreviewAdapter bottomPreviewAdapter;

    private boolean isShowCamera;

    /**
     * EventBus 3.0 回调
     *
     * @param obj
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBus(EventEntity obj) {
        switch (obj.what) {
            case PictureConfig.CLOSE_PREVIEW_FLAG:
                // 压缩完后关闭预览界面
                dismissDialog();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                }, 150);
                break;
        }
    }

    @Override
    public void immersive() {
        ImmersiveManage.immersiveAboveAPI23(this
                , Color.TRANSPARENT
                , Color.TRANSPARENT
                , openWhiteStatusBar);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_preview);
        if (!RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().register(this);
        }
        mHandler = new Handler();
        screenWidth = ScreenUtils.getScreenWidth(this);
        animation = OptAnimationLoader.loadAnimation(this, R.anim.modal_in);
        animation.setAnimationListener(this);
        picture_left_back = (ImageView) findViewById(R.id.picture_left_back);
        viewPager = (PreviewViewPager) findViewById(R.id.preview_pager);
        ll_check = (LinearLayout) findViewById(R.id.ll_check);
        check = (TextView) findViewById(R.id.check);
        picture_left_back.setOnClickListener(this);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        tv_ok.setOnClickListener(this);
        tv_title = (TextView) findViewById(R.id.picture_title);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);


        selectImages = (List<LocalMedia>) getIntent().
                getSerializableExtra(PictureConfig.EXTRA_SELECT_LIST);
        isShowCamera = getIntent().getBooleanExtra("showCamera", true);
        is_bottom_preview = getIntent().
                getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false);
        if (is_bottom_preview) {
            // 底部预览按钮过来
            images = (List<LocalMedia>) getIntent().
                    getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        } else {
            images = ImagesObservable.getInstance().readLocalMedias();
        }
        initBottomRecycleView();
        initViewPageAdapterData();

        ll_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (images != null && images.size() > 0) {
                    LocalMedia image = images.get(viewPager.getCurrentItem());
                    String pictureType = selectImages.size() > 0 ?
                            selectImages.get(0).getPictureType() : "";
                    if (!TextUtils.isEmpty(pictureType)) {
                        boolean toEqual = PictureMimeType.
                                mimeToEqual(pictureType, image.getPictureType());
                        if (!toEqual) {
                            ToastManage.s(mContext, getString(R.string.picture_rule));
                            return;
                        }
                    }
                    // 刷新图片列表中图片状态
                    boolean isChecked;
                    if (!check.isSelected()) {
                        isChecked = true;
                        check.setSelected(true);
                        check.startAnimation(animation);
                    } else {
                        isChecked = false;
                        check.setSelected(false);
                    }
                    if (selectImages.size() >= config.maxSelectNum && isChecked) {
                        ToastManage.s(mContext, getString(R.string.picture_message_max_num, config.maxSelectNum));
                        check.setSelected(false);
                        return;
                    }
                    if (isChecked) {
                        VoiceUtils.playVoice(mContext, config.openClickSound);
                        // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                        if (config.selectionMode == PictureConfig.SINGLE) {
                            singleRadioMediaImage();
                        }
                        image.setChecked(true);
                        selectImages.add(image);
                        image.setNum(selectImages.size());
                        if (config.checkNumMode) {
                            check.setText(String.valueOf(image.getNum()));
                        }
                    } else {
                        for (LocalMedia media : selectImages) {
                            if (media.getPath().equals(image.getPath())) {
                                selectImages.remove(media);
                                subSelectPosition();
                                notifyCheckChanged(media);
                                break;
                            }
                        }
                    }
                    bottomPreviewAdapter.referesh(selectImages);
                    bottomPreviewAdapter.notifyDataSetChanged();
                    onSelectNumChange(true);
                }
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                isPreviewEggs(config.previewEggs, position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int i) {
                position = i;
                tv_title.setText(position + 1 + "/" + images.size());
                LocalMedia media = images.get(position);
                index = media.getPosition();
                if (!config.previewEggs) {
                    if (config.checkNumMode) {
                        check.setText(media.getNum() + "");
                        notifyCheckChanged(media);
                    }
                    onImageChecked(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void initBottomRecycleView() {
        recycleBottom = findViewById(R.id.recycle_bottom);
        recycleBottom.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        if(selectImages.size() == 0){
            recycleBottom.setVisibility(View.GONE);
        }
        if(is_bottom_preview){
            selectImages.get(0).setChecked(true);
            recycleBottom.smoothScrollToPosition(0);
        }else {
            LocalMedia media = images.get(position);
            if(selectImages.size() != 0){
                for (int i = 0 ; i< selectImages.size() ; i++){
                    if(media.getPath().equals(selectImages.get(i).getPath())){
                        selectImages.get(i).setChecked(true);
                    }
                }
            }
        }
        bottomPreviewAdapter = new BottomPreviewAdapter(this, selectImages);
        recycleBottom.setAdapter(bottomPreviewAdapter);
        bottomPreviewAdapter.setOnItemClcikLitener(new BottomPreviewAdapter.OnItemClcikLitener() {
            @Override
            public void OnItemClcik(int position, LocalMedia image) {
                if(is_bottom_preview){
                    viewPager.setCurrentItem(position, false);
                }else {
                    int v_position = isShowCamera ? selectImages.get(position).getPosition() - 1 : selectImages.get(position).getPosition();
                    viewPager.setCurrentItem(v_position, false);
                }
                bottomPreviewAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 这里没实际意义，好处是预览图片时 滑动到屏幕一半以上可看到下一张图片是否选中了
     *
     * @param previewEggs          是否显示预览友好体验
     * @param positionOffsetPixels 滑动偏移量
     */
    private void isPreviewEggs(boolean previewEggs, int position, int positionOffsetPixels) {
        if (previewEggs) {
            if (images.size() > 0 && images != null) {
                LocalMedia media;
                int num;
                if (positionOffsetPixels < screenWidth / 2) {
                    media = images.get(position);
                    check.setSelected(isSelected(media));
                    if (config.checkNumMode) {
                        num = media.getNum();
                        check.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position);
                    }
                } else {
                    media = images.get(position + 1);
                    check.setSelected(isSelected(media));
                    if (config.checkNumMode) {
                        num = media.getNum();
                        check.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position + 1);
                    }
                }
            }
        }
    }

    /**
     * 单选图片
     */
    private void singleRadioMediaImage() {
        if (selectImages != null
                && selectImages.size() > 0) {
            LocalMedia media = selectImages.get(0);
            RxBus.getDefault()
                    .post(new EventEntity(PictureConfig.UPDATE_FLAG,
                            selectImages, media.getPosition()));
            selectImages.clear();
        }
    }

    /**
     * 初始化ViewPage数据
     */
    private void initViewPageAdapterData() {
        tv_title.setText(position + 1 + "/" + images.size());
        adapter = new SimpleFragmentAdapter(images, this, this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);
        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            index = media.getPosition();
            if (config.checkNumMode) {
                check.setText(media.getNum() + "");
                notifyCheckChanged(media);
            }
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (config.checkNumMode) {
            check.setText("");
            for (LocalMedia media : selectImages) {
                media.setChecked(false);
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    check.setText(String.valueOf(imageBean.getNum()));
                    selectImages.get(media.getSelectPosition()).setChecked(true);
                }
            }
            bottomPreviewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0) {
            LocalMedia media = images.get(position);
            check.setSelected(isSelected(media));
        } else {
            check.setSelected(false);
        }

        LocalMedia localMedia = images.get(position);
        for (LocalMedia media : selectImages) {
            media.setChecked(false);
            if (media.getPath().equals(localMedia.getPath())) {
                selectImages.get(media.getSelectPosition()).setChecked(true);
            }
        }
        bottomPreviewAdapter.notifyDataSetChanged();
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */

    public void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        if (enable) {
            tv_ok.setText("完成(" + selectImages.size() + ")");
            tv_ok.setTextColor(Color.WHITE);
            recycleBottom.setVisibility(View.VISIBLE);
        } else {
            tv_ok.setText("完成");
            tv_ok.setTextColor(Color.GRAY);
            recycleBottom.setVisibility(View.GONE);
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            EventEntity obj = new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, index);
            RxBus.getDefault().post(obj);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        updateSelector(refresh);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.picture_left_back) {
            onBackPressed();
        }
        if (id == R.id.tv_ok) {
            // 如果设置了图片最小选择数量，则判断是否满足条件
            int size = selectImages.size();
            if(size == 0){
                ToastManage.s(mContext, "没有选择任何内容");
                return;
            }
            LocalMedia image = selectImages.size() > 0 ? selectImages.get(0) : null;
            String pictureType = image != null ? image.getPictureType() : "";
            if (config.minSelectNum > 0) {
                if (size < config.minSelectNum && config.selectionMode == PictureConfig.MULTIPLE) {
                    boolean eqImg = pictureType.startsWith(PictureConfig.IMAGE);
                    String str = eqImg ? getString(R.string.picture_min_img_num, config.minSelectNum)
                            : getString(R.string.picture_min_video_num, config.minSelectNum);
                    ToastManage.s(mContext, str);
                    return;
                }
            }
            if (config.enableCrop && pictureType.startsWith(PictureConfig.IMAGE)) {
                if (config.selectionMode == PictureConfig.SINGLE) {
                    originalPath = image.getPath();
                    startCrop(originalPath);
                } else {
                    // 是图片和选择压缩并且是多张，调用批量压缩
                    ArrayList<String> cuts = new ArrayList<>();
                    for (LocalMedia media : selectImages) {
                        cuts.add(media.getPath());
                    }
                    startCrop(cuts);
                }
            } else {
                onResult(selectImages);
            }
        }
    }

    @Override
    public void onResult(List<LocalMedia> images) {
        RxBus.getDefault().post(new EventEntity(PictureConfig.PREVIEW_DATA_FLAG, images));
        // 如果开启了压缩，先不关闭此页面，PictureImageGridActivity压缩完在通知关闭
        if (!config.isCompress) {
            onBackPressed();
        } else {
            showPleaseDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode == RESULT_OK) {
//            switch (requestCode) {
//                case UCropMulti.REQUEST_MULTI_CROP:
//                    List<CutInfo> list = UCropMulti.getOutput(data);
//                    setResult(RESULT_OK, new Intent().putExtra(UCropMulti.EXTRA_OUTPUT_URI_LIST,
//                            (Serializable) list));
//                    finish();
//                    break;
//                case UCrop.REQUEST_CROP:
//                    if (data != null) {
//                        setResult(RESULT_OK, data);
//                    }
//                    finish();
//                    break;
//            }
//        } else if (resultCode == UCrop.RESULT_ERROR) {
//            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
//            ToastManage.s(mContext, throwable.getMessage());
//        }
    }


    @Override
    public void onBackPressed() {
        closeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().unregister(this);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    @Override
    public void onActivityBackPressed() {
        onBackPressed();
    }
}
