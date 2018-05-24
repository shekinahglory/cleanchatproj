package com.shekglory.friends;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {



    private ImageView mProfileImageView;

    private TextView mProfileName , mProfileStatus , mProfileFriendsCount;
    private Button mProfileSendRequestBtn, mDeclineBtn;
    private DatabaseReference nUserDatabase;
    private ProgressDialog progressDialog;
    private String currentState;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mRootRef = FirebaseDatabase.getInstance().getReference();

        nUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notification");
        mProfileImageView = (ImageView) findViewById(R.id.profileImageViewId);
        mProfileStatus = (TextView) findViewById(R.id.profileDisplayStatus);
        mProfileName = (TextView) findViewById(R.id.profileDisplayName);
        mProfileFriendsCount = (TextView) findViewById(R.id.profileTotalFriendsId);
        mProfileSendRequestBtn = (Button) findViewById(R.id.profileSendRequestBtn);
        mDeclineBtn = (Button) findViewById(R.id.profileDecineRequestBtn);
        mDeclineBtn.setVisibility(View.INVISIBLE);


        currentState = "not_friends";

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading User Data");
        progressDialog.setMessage("Please wait while we load the user data.");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();

        nUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String display_name = dataSnapshot.child("name").getValue().toString();

                String status = dataSnapshot.child("status").getValue().toString();

                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.defaultimg).into(mProfileImageView);


//                friends list / request feature

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {


                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){

                            String req_stype = dataSnapshot.child(user_id).child("request_type").getValue().toString();


                            if(req_stype.equals("received")){


                                currentState = "req_received";
                                mProfileSendRequestBtn.setText("Accept Friend Request");
                                progressDialog.dismiss();
                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);

                            } else if (req_stype.equals("sent")){
                                currentState = "req_sent";
                                mProfileSendRequestBtn.setText("Cancel Friend Request!");
                                progressDialog.dismiss();
                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }

                            progressDialog.dismiss();

                        } else {
                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if (dataSnapshot.hasChild(user_id)){
                                        currentState = "friends";
                                        mProfileSendRequestBtn.setText("UNFRIEND THIS PERSON");
                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);

                                    }
                                    progressDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    progressDialog.dismiss();

                                }
                            });
                        }

                    }



                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendRequestBtn.setEnabled(false);


//              NOt FRIEND YET THEN SEND REQUEST

                if(currentState.equals("not_friends")){

                    DatabaseReference newNotificationRef = mRootRef.child("notification").child(user_id).push();
                    String newNotificationId = newNotificationRef.getKey();


                    HashMap<String, String> notificationData = new HashMap<>();

                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();

                    requestMap.put( "Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                    requestMap.put( "Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("notification/" + user_id + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null){
                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_LONG).show();
                            }
                            mProfileSendRequestBtn.setEnabled(true);
                            currentState = "req_sent";
                            mProfileSendRequestBtn.setText("Cancel Friend Request");
                            mDeclineBtn.setVisibility(View.INVISIBLE);
                            mDeclineBtn.setEnabled(false);

                        }
                    });
                }

//              CANCEL REQUEST STATE

                if(currentState.equals("req_sent")){

                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendRequestBtn.setEnabled(true);
                                    currentState = "not_friends";
                                    mProfileSendRequestBtn.setText("SEND FRIEND REQUEST");
                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);


                                }
                            });
                        }
                    });
                }

//              Request received state

                if(currentState.equals("req_received")){

                    final String currenDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();

                    friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/" + "/date" , currenDate);
                    friendsMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/" + "/date", currenDate);

                    friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){
                                mProfileSendRequestBtn.setEnabled(true);
                                currentState = "friends";
                                mProfileSendRequestBtn.setText("Unfriend this Person");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                            }

                        }
                    });


//

                }

                // unfriendings

                if(currentState.equals("friends")){
                    Map unfriendMap = new HashMap();

                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError == null){

                                currentState = "not_friends";
                                mProfileSendRequestBtn.setText("Send Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                            }

                            mProfileSendRequestBtn.setEnabled(true);

                        }
                    });


                }
            }
        });



    }
}
