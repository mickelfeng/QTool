package com.hicore.qtool.XposedInit;

import android.content.Context;

import com.hicore.HookUtils.XPBridge;
import com.hicore.LogUtils.LogUtils;
import com.hicore.ReflectUtils.ResUtils;
import com.hicore.ReflectUtils.MClass;
import com.hicore.qtool.HookEnv;
import com.hicore.qtool.XposedInit.ItemLoader.HookLoader;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class EnvHook {
    private static final String TAG = "EnvHook";
    public static void HookForContext(){
        //由于很多环境的初始化都需要Context来进行,所有这里选择直接Hook获取Context再进行初始化
        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.qfix.QFixApplication", HookEnv.mLoader, "attachBaseContext", Context.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                HookEnv.AppContext = (Context) param.args[0];

                //取代QQ的classLoader防止有一些框架传递了不正确的classLoader
                HookEnv.mLoader = param.thisObject.getClass().getClassLoader();

                ClassLoader fixLoader = EnvHook.class.getClassLoader().getParent();
                if (fixLoader instanceof HookEntry.FixSubClassLoader){
                    LogUtils.debug(TAG,"Init from FixSubClassLoade");
                    HookEnv.fixLoader = (HookEntry.FixSubClassLoader) fixLoader;
                }
                //优先初始化Path
                ExtraPathInit.InitPath();


                //然后注入资源
                LogUtils.debug(TAG,"BaseHook Start");
                ResUtils.StartInject(HookEnv.AppContext);
                //然后进行延迟Hook,同时如果目录未设置的时候能弹出设置界面
                HookForDelayDialog();
                if (HookEnv.ExtraDataPath != null){
                    //在外部数据路径不为空且有效的情况下才加载Hook,防止意外导致的设置项目全部丢失
                    HookLoader.SearchAndLoadAllHook();
                }
                LogUtils.debug(TAG,"BaseHook End");

            }
        });
    }
    private static void HookForDelayDialog(){
        XPBridge.HookBeforeOnce(XposedHelpers.findMethodBestMatch(MClass.loadClass("com.tencent.mobileqq.startup.step.LoadData"),"doStep"),param -> {
            LogUtils.debug(TAG,"DelayHook Start");
            if (HookEnv.ExtraDataPath == null) ExtraPathInit.ShowPathSetDialog();
            else HookLoader.CallAllDelayHook();
            LogUtils.debug(TAG,"DelayHook End");
        });
    }
}