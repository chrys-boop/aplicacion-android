package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private final List<Conversation> conversationList;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationsAdapter(List<Conversation> conversationList, OnConversationClickListener listener) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.bind(conversation, listener);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView userName, lastMessage, time;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.image_view_avatar);
            userName = itemView.findViewById(R.id.text_view_user_name);
            lastMessage = itemView.findViewById(R.id.text_view_last_message);
            time = itemView.findViewById(R.id.text_view_time);
        }

        void bind(final Conversation conversation, final OnConversationClickListener listener) {
            userName.setText(conversation.getOtherUserName());
            lastMessage.setText(conversation.getLastMessage());

            // *** INICIO DE LA CORRECCIÓN DE ZONA HORARIA ***
            if (conversation.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                // Establecer explícitamente la zona horaria de la Ciudad de México
                sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
                time.setText(sdf.format(new Date(conversation.getTimestamp())));
            } else {
                time.setText("");
            }
            // *** FIN DE LA CORRECION DE ZONA HORARIA ***

            // Se podría cargar una imagen de avatar real aquí
            // avatar.setImageResource(...);

            itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}


