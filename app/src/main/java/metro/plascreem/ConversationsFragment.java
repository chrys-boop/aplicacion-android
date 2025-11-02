package metro.plascreem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationsFragment extends Fragment implements ConversationsAdapter.OnConversationClickListener {

    private static final String TAG = "ConversationsFragment";

    private RecyclerView recyclerView;
    private ConversationsAdapter adapter;
    private List<Conversation> conversationList = new ArrayList<>();
    private String currentUserId;

    private DatabaseReference conversationsRef;
    private ValueEventListener valueEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            return view;
        }

        recyclerView = view.findViewById(R.id.recycler_view_conversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConversationsAdapter(conversationList, this);
        recyclerView.setAdapter(adapter);

        loadConversations();

        return view;
    }

    private void loadConversations() {
        conversationsRef = FirebaseDatabase.getInstance().getReference("direct_messages");

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                conversationList.clear();
                Map<String, Conversation> latestConversations = new HashMap<>();

                for (DataSnapshot chatNodeSnapshot : dataSnapshot.getChildren()) {
                    String chatNodeId = chatNodeSnapshot.getKey();

                    // *** INICIO DE LA CORRECCIÓN: Lógica robusta para obtener otherUserId ***
                    if (chatNodeId != null && chatNodeId.contains(currentUserId)) {
                        String[] userIds = chatNodeId.split("_");
                        String otherUserId = null;
                        if (userIds.length == 2) {
                            if (userIds[0].equals(currentUserId)) {
                                otherUserId = userIds[1];
                            } else {
                                otherUserId = userIds[0];
                            }
                        }

                        if (TextUtils.isEmpty(otherUserId)) {
                            continue; // Si no podemos identificar al otro usuario, saltamos este chat.
                        }
                        // *** FIN DE LA CORRECCIÓN ***

                        DirectMessage lastMessage = null;
                        for (DataSnapshot messageSnapshot : chatNodeSnapshot.getChildren()) {
                            lastMessage = messageSnapshot.getValue(DirectMessage.class);
                        }

                        if (lastMessage != null) {
                            Conversation conversation = new Conversation(otherUserId, "", lastMessage.getMessage(), lastMessage.getTimestamp());
                            latestConversations.put(otherUserId, conversation);
                        }
                    }
                }

                fetchUserNamesAndPopulate(new ArrayList<>(latestConversations.values()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error al cargar conversaciones", databaseError.toException());
            }
        };
        conversationsRef.addValueEventListener(valueEventListener);
    }

    private void fetchUserNamesAndPopulate(List<Conversation> conversations) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        if (conversations.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        int totalConversations = conversations.size();
        int processedConversations = 0;

        for (Conversation conversation : conversations) {
            usersRef.child(conversation.getOtherUserId()).child("nombreCompleto").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String userName = dataSnapshot.getValue(String.class);
                    conversation.setOtherUserName(userName != null ? userName : "Usuario desconocido");
                    conversationList.add(conversation);

                    if (conversationList.size() == totalConversations) {
                        Collections.sort(conversationList, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error al cargar el nombre de usuario", databaseError.toException());
                    conversation.setOtherUserName("Error al cargar");
                    conversationList.add(conversation);

                    if (conversationList.size() == totalConversations) {
                        Collections.sort(conversationList, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("senderId", conversation.getOtherUserId());
        intent.putExtra("otherUserName", conversation.getOtherUserName());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationsRef != null && valueEventListener != null) {
            conversationsRef.removeEventListener(valueEventListener);
        }
    }
}