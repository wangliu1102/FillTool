package com.wl.android.filltool.fragment;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wl.android.filltool.R;

import java.util.ArrayList;

/**
 * Created by D22397 on 2017/9/4.
 * 联系人填充
 */

public class ContactFillFragment extends Fragment {
    int num;
    private static final String TAG = "ContactFillFragment";
    private static final String Contact_URI = "content://com.android.contacts/raw_contacts"; // 通讯录
    private static final String Contact_URI2 = "content://icc/adn";//SIM卡
    private static String[] sTelFirst = ("134,135,136,137,138,139,150,151,152,157" +
            ",158,159,130,131,132,155,156,133,153").split(",");
    private static String[] sFirstName = ("赵,钱,孙,李,周,刘,昌,马,上官,苗,凤,花,方,张夏,俞,任,袁,柳,酆,鲍,史" +
            ",费,廉,岑,薛,雷,贺,倪,汤,凤,花,方,俞,任,袁,柳,酆,鲍,史,唐,费,廉" +
            ",薛,雷,贺,倪,汤,郝,邬,安,欧阳,乐,曹,于,时,傅,皮,卞,齐,康" +
            ",余,元,卜,顾,孟,平,黄,和,穆,萧,尹,姚,邵,湛,汪,祁,毛,禹,狄,米,贝" +
            ",臧,计,成,戴,谈,宋,茅,庞,熊,纪,迪丽,舒,屈,项,祝,董,梁,杜,阮,蓝,闵,席" +
            ",麻,强,贾,路,娄,危,江,童,颜,郭,梅,盛,林,刁,徐,邱,骆,高,夏,蔡" +
            ",樊,胡,凌,霍,虞,章,支,柯,薛,徐,艾克,管,卢,莫,经,房,裘,缪,干,解,应,宗,宣" +
            ",贲,邓,郁,单,杭,洪,包,诸,石,崔,吉,钮,龚,程,嵇,邢,滑,裴,陆,荣" +
            ",羊,惠,甄,魏,加,封,芮,羿,靳,汲,邴,糜,松,井,段,司马,富,巫,乌,焦,巴" +
            ",隗,山,谷,车,侯,宓").split(",");
    private static String[] sLastName = ("霞霞,话,小,发,德华,杰伦,秀,娟,小小,华,慧,巧,美,娜,静,淑,惠" +
            ",珠,翠,雅,芝,玉,果果,红,娥,玲,江江,芳,燕,彩,春,里里,菊,丹丹,凤,洁,梅,琳,素,云" +
            ",莲,真,环,雪,荣,嘉雨,爱,妹,霞,香,月,莺,媛,娜娜,瑞,佳,嘉,琼,勤,珍,贞,莉,桂" +
            ",娣,叶,璧,娅,琦,易丹,晶,妍,茜,秋,热巴,莎,锦,黛,青,倩,婷,姣,云遥,婉,娴,瑾,颖" +
            ",露,瑶,怡,婵,雁,蓓,纨,仪,静蕾,丹,蓉,之谦,眉,好,子怡,将,江,丹,红,雨,静,笑").split(",");

    Context context;
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;

    private TextView mContactsNumTextView;
    private TextView mContactsNumTextView2;
    private EditText mAddNumEditText;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mProgressDialog1;
    private ProgressDialog mProgressDialog2;
    private ProgressDialog mProgressDialog3;
    private ProgressDialog mProgressDialog4;

    private boolean mIsAdd = false;
    private boolean mIsDelete = false;
    private int num2 = 0;
    ArrayList<String> telList;
    ArrayList<Long> idLists;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contact_fill, container, false);
        mAddNumEditText = (EditText) v.findViewById(R.id.add_num_edit_text);
        mContactsNumTextView = (TextView) v.findViewById(R.id.contacts_num_text_view);
        mContactsNumTextView2 = (TextView) v.findViewById(R.id.contacts_num_text_view2);
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("填充中，请稍候....");
        mProgressDialog.setTitle("填充联系人");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        mProgressDialog1 = new ProgressDialog(getActivity());
        mProgressDialog1.setMessage("删除中，请稍候....");
        mProgressDialog1.setTitle("清空本地联系人");
        mProgressDialog1.setIndeterminate(true);
        mProgressDialog1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog1.setCancelable(false);

        mProgressDialog2 = new ProgressDialog(getActivity());
        mProgressDialog2.setMessage("填充中，请稍候....");
        mProgressDialog2.setTitle("填充SIM卡联系人");
        mProgressDialog2.setIndeterminate(true);
        mProgressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog2.setCancelable(false);

        mProgressDialog3 = new ProgressDialog(getActivity());
        mProgressDialog3.setMessage("删除中，请稍候....");
        mProgressDialog3.setTitle("清空SIM卡联系人");
        mProgressDialog3.setIndeterminate(true);
        mProgressDialog3.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog3.setCancelable(false);

        mProgressDialog4 = new ProgressDialog(getActivity());
        mProgressDialog4.setMessage("正在填充联系人ID，请稍后....");
        mProgressDialog4.setTitle("填充联系人ID");
        mProgressDialog4.setIndeterminate(true);
        mProgressDialog4.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog4.setCancelable(false);

        context = getActivity();
        btn1 = (Button) v.findViewById(R.id.AddButton);
        btn1.setEnabled(true);
        btn2 = (Button) v.findViewById(R.id.DeleteButton);
        btn2.setEnabled(true);
        updateNumContacts();
        btn3 = (Button) v.findViewById(R.id.AddButton2);
        btn3.setEnabled(true);
        btn4 = (Button) v.findViewById(R.id.DeleteButton2);
        btn4.setEnabled(true);

        //填充本地按钮的监听事件
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mEdit = mAddNumEditText.getText().toString();
                if (!TextUtils.isEmpty(mEdit)) {
                    num = Integer.parseInt(mEdit);
                    if (!mIsAdd) {
                        final AddContactId addContactId = new AddContactId();
                        addContactId.execute();       //启动异步任务
                    }

                } else {
                    Toast.makeText(getActivity(), "Please input a number！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //清空本地联系人按钮的监听事件
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // deleteContacts();
                if (!mIsDelete) {
                    final DeleteContactTask deleteContactTask = new DeleteContactTask();
                    deleteContactTask.execute();
                }
            }
        });
        //填充SIM卡的按钮事件
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mEdit = mAddNumEditText.getText().toString();
                if (!TextUtils.isEmpty(mEdit)) {
                    num = Integer.parseInt(mEdit);
                    if (!mIsAdd) {
                        final AddContactTask2 addContactTask2 = new AddContactTask2();
                        addContactTask2.execute(num);       //启动异步任务
                    }

                } else {
                    Toast.makeText(getActivity(), "Please input a number！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //清空SIM卡联系人的按钮
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsDelete) {
                    final DeleteContactTask2 deleteContactTask2 = new DeleteContactTask2();
                    deleteContactTask2.execute();
                }

            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNumContacts();
        updateNumContacts2();
    }

    public int getNum(int start, int end) {
        return (int) (Math.random() * (end - start + 1) + start);
    }

    //返回手机号码
    private String getTel() {
        int index = getNum(0, sTelFirst.length - 1);
        String first = sTelFirst[index];
        String second = String.valueOf(getNum(1, 888) + 10000).substring(1);
        String third = String.valueOf(getNum(1, 9100) + 10000).substring(1);
        return first + second + third;
    }

    //返回姓名
    private String getName() {
        int index = getNum(0, sFirstName.length - 1);
        String firstName = sFirstName[index];
        int index2 = getNum(0, sLastName.length - 1);
        String lastName = sLastName[index2];
        Log.d(TAG, "getName: " + firstName + ":" + lastName);
        return firstName + lastName;
    }

    //更新本地联系人
    private void updateNumContacts() {
        mContactsNumTextView.setText("本地联系人数：" + String.valueOf(getContactsNum()));
    }

    //更新SIM卡联系人
    private void updateNumContacts2() {
        mContactsNumTextView2.setText("SIM卡联系人数：" + String.valueOf(getContactsNum2()));
    }

    //获取本地当前联系人数
    private int getContactsNum() {
        int count = 0;
        Uri uri = Uri.parse(Contact_URI);
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                count = cursor.getCount();
                Log.d(TAG, "count:" + count);
            }
        } catch (SQLiteException e) {
            Log.d(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    //获取SIM卡当前联系人数
    private int getContactsNum2() {
        int count = 0;
        Uri uri = Uri.parse(Contact_URI2);
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                count = cursor.getCount();
                Log.d(TAG, "count:" + count);
            }
        } catch (SQLiteException e) {
            Log.d(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    //异步任务1（添加到SIM卡）
    class AddContactTask2 extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog2.setMax(num);
            mProgressDialog2.show();
            mIsAdd = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) { //执行耗时操作
            Uri uri = Uri.parse("content://icc/adn");
            ContentValues values = new ContentValues();
            Uri uri5;
            ContentResolver resolver1 = getActivity().getContentResolver();
            int i;
            for (i = 0; i < num; i++) {
                values.put("tag", getName());
                values.put("number", getTel());
                uri5 = resolver1.insert(uri, values);
                if (uri5 == null) {
                    return 0;
                }
                publishProgress((int) (((i + 1) * 100) / num));
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {  //更新UI界面
            super.onProgressUpdate(values);
            mProgressDialog2.setProgress(values[0]);
            mProgressDialog2.setIndeterminate(false);
        }

        @Override
        protected void onPostExecute(Integer integer) {   //收尾工作
            super.onPostExecute(integer);
            mIsAdd = false;
            mProgressDialog2.dismiss();
            switch (integer) {
                case 0:
                    Toast.makeText(getActivity(), "Insert Failed！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateNumContacts2();
                    Toast.makeText(getActivity(), "Insert Successfully！", Toast.LENGTH_SHORT).show();
                    mProgressDialog2.incrementProgressBy(-mProgressDialog2.getProgress());
                    break;
                default:
                    break;
            }
        }
    }

    class AddContactId extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog4.show();
        }


        @Override
        protected Boolean doInBackground(Void... voids) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
            telList = new ArrayList<>();
            for (int i = 0; i < num; i++) {
                String tel = getTel();
                boolean isContains = true;
                while (isContains) {
                    if (!telList.contains(tel)) {
                        telList.add(getTel());
                        isContains = false;
                    } else {
                        tel = getTel();
                        isContains = true;
                    }
                }
            }
            Log.d(TAG, telList.toString());
            Log.d(TAG, "idLists: 开始插ID");

            long id;
            idLists = new ArrayList<>();
            ContentValues values = new ContentValues();
            for (int i = 0; i < telList.size(); i++) {
                id = ContentUris.parseId(resolver.insert(uri, values));
                idLists.add(id);
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog4.dismiss();
            final AddContactTask addContactTask = new AddContactTask();
            addContactTask.execute(num);       //启动异步任务
        }

    }

    //异步任务2(添加联系人到本地）
    class AddContactTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMax(num);
            mProgressDialog.show();
            mIsAdd = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) { //执行耗时操作
            int num = params[0];
            ContentResolver resolver = context.getContentResolver();
            Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
//            for (int i = 0; i < num; i++) {
//                String tel = getTel();
//                boolean isContains = true;
//                while (isContains) {
//                    if (!telList.contains(tel)) {
//                        telList.add(getTel());
//                        isContains = false;
//                    } else {
//                        tel = getTel();
//                        isContains = true;
//                    }
//                }
//            }
//            Log.d(TAG, telList.toString());
//            Log.d(TAG, "idLists: 开始插ID");
//
//            long id;
            ContentValues values = new ContentValues();
//            for (int i = 0; i < telList.size(); i++) {
//                id = ContentUris.parseId(resolver.insert(uri, values));
//                idLists.add(id);
//            }
            Log.d(TAG, "idLists: 开始插联系人" + idLists.toString());
            Log.d(TAG, "telList: 开始插联系人" + telList.toString());
            for (int j = 0; j < idLists.size(); j++) {
                uri = Uri.parse("content://com.android.contacts/data");
                //添加联系人姓名
                values.put("raw_contact_id", idLists.get(j));
                values.put(ContactsContract.Contacts.Data.MIMETYPE, "vnd.android.cursor.item/name");
                values.put("data2", getName());
                resolver.insert(uri, values);
                values.clear();
                // 添加联系人电话
                values.put("raw_contact_id", idLists.get(j));
                values.put(ContactsContract.Contacts.Data.MIMETYPE, "vnd.android.cursor.item/phone_v2");
                values.put("data1", telList.get(j));
                values.put("data2", "2");
                Uri uri1 = resolver.insert(uri, values);
                values.clear();
                if (uri1 == null) {
                    return 0;
                }
                publishProgress(j + 1);
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {  //更新UI界面
            super.onProgressUpdate(values);
            mProgressDialog.setProgress(values[0]);
            mProgressDialog.setIndeterminate(false);
        }

        @Override
        protected void onPostExecute(Integer integer) {   //收尾工作
            super.onPostExecute(integer);
            mProgressDialog.dismiss();
            mIsAdd = false;
            switch (integer) {
                case 0:
                    Toast.makeText(getActivity(), "Insert Failed！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateNumContacts();
                    Toast.makeText(getActivity(), "Insert Successfully！", Toast.LENGTH_SHORT).show();
                    mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
                    break;
                default:
                    break;
            }
        }
    }

    //异步任务3（删除本地联系人）
    class DeleteContactTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {    //第一步执行
            super.onPreExecute();
            mProgressDialog1.show();
            mIsDelete = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) {    //第二步执行耗时操作
            Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor cursor = resolver.query(uri, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                cursor.moveToNext();
                resolver.delete(uri, null, null);
                if (uri == null) {
                    return 0;
                }
            }
            cursor.close();
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {   //收尾工作
            super.onPostExecute(integer);
            mProgressDialog1.dismiss();
            mIsDelete = false;
            switch (integer) {
                case 0:
                    Toast.makeText(getActivity(), "Delete Failed！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateNumContacts();
                    Toast.makeText(getActivity(), "Delete Successfully！", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    //异步任务4（删除SIM卡联系人）
    class DeleteContactTask2 extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {    //第一步执行
            super.onPreExecute();
            mProgressDialog3.show();
            mIsDelete = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) {    //第二步执行耗时操作
            Uri uri = Uri.parse(Contact_URI2);
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null,
                    null, null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(Contacts.People.NAME));
                String phoneNumber = cursor.getString(cursor
                        .getColumnIndex(Contacts.People.NUMBER));
                String where = "tag='" + name + "'";
                where += " AND number='" + phoneNumber + "'";
                getActivity().getContentResolver().delete(uri, where, null);
                Log.d(TAG, "doInBackground: hahajhs");
                if (uri == null) {
                    return 0;
                }
            }
            cursor.close();
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {   //收尾工作
            super.onPostExecute(integer);
            mProgressDialog3.dismiss();
            mIsDelete = false;
            switch (integer) {
                case 0:
                    Toast.makeText(getActivity(), "Delete Failed！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateNumContacts2();
                    Toast.makeText(getActivity(), "Delete Successfully！", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }
}
