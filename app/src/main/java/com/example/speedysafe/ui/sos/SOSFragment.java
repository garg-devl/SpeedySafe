package com.example.speedysafe.ui.sos;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.speedysafe.ItemEmergencyContact;
import com.example.speedysafe.databinding.AddContactNumberDialogBinding;
import com.example.speedysafe.databinding.FragmentSosBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.util.ArrayList;

public class SOSFragment extends Fragment {

    private FragmentSosBinding binding;
    DatabaseReference reference;
    SharedPreferences preferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    String getItemFromSharedPreference(String key){
        return preferences.getString(key, "");
    }

    @Override
    public void onStart() {

        preferences = requireContext().getSharedPreferences("SpeedySafe", MODE_PRIVATE);

        String contactNumber = getItemFromSharedPreference("contactNumber");

        reference = FirebaseDatabase.getInstance("https://speedysafe-89d96-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference(contactNumber);

        binding.editEmergencyContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddContactNumberDialogBinding binding = AddContactNumberDialogBinding.inflate(LayoutInflater.from(requireContext()));
                //initialize alert builder.
                AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());

                //set our custom alert dialog to tha alertdialog builder
                alert.setView(binding.getRoot());

                final AlertDialog dialog = alert.create();

                binding.addContactButton.setOnClickListener(view1 -> {
                    String name = binding.contactNameEnter.getText().toString();
                    String relation = binding.relationEnter.getText().toString();
                    String contact = binding.mobileNoEnter.getText().toString();

                    if(name.isEmpty() || relation.isEmpty() || contact.isEmpty()){
                        Toast.makeText(requireContext(), "Can't be empty", Toast.LENGTH_SHORT).show();
                    }else {
                        ItemEmergencyContact contactItem = new ItemEmergencyContact(contact, name, relation);
                        reference.child("EmergencyContact").setValue(contactItem).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        getDataFromFirebase();
        getNearestHospital();
        super.onStart();
    }

    private void getNearestHospital() {
        ItemHospital hospital = findNearestHospital();

        binding.nameNearbyHospital.setText(hospital.Name);
        binding.nearestHospitalAddress.setText(hospital.Address);

        binding.callEmergencyContact.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + hospital.MobileNumber));
            requireContext().startActivity(intent);
        });
    }

    void getDataFromFirebase(){

        reference.child("EmergencyContact").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    ItemEmergencyContact contact = snapshot.getValue(ItemEmergencyContact.class);

                    binding.nameEmergencyContact.setText(contact.ContactName);
                    binding.relationEmergencyContact.setText(contact.Relation);
                    binding.numberEmergencyContact.setText(contact.Contact);

                    binding.callEmergencyContact.setOnClickListener(view -> {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + contact.Contact));
                        requireContext().startActivity(intent);
                    });

                }else{
                    binding.nameEmergencyContact.setText("NA");
                    binding.relationEmergencyContact.setText("NA");
                    binding.numberEmergencyContact.setText("NA");
                    binding.callEmergencyContact.setOnClickListener(view -> {
                        Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT).show();
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ItemHospital findNearestHospital(){

        ArrayList<ItemHospital> hospitalArrayList = new ArrayList<>();
        hospitalArrayList.add(new ItemHospital("Sant Soham hospital", "Rithala Road, Pocket 4, Sector 11E, Rohini, Delhi, 110085", "NA", 28.735617,77.112993));
        hospitalArrayList.add(new ItemHospital("Super Speciality Hospital Government Medical College Jammu", "Resham Ghar Colony, Jammu, Jammu and Kashmir 180016", "+911912520816", 32.730216,74.857419));
        hospitalArrayList.add(new ItemHospital("Shri Maharaja Gulab Singh Hospital", "Shalamar, Resham Ghar Colony, Jammu, Jammu and Kashmir 180001", "+911912547635", 32.733135,74.860836));
        hospitalArrayList.add(new ItemHospital("Government Medical College and hospital", "maheshpura, chowk, Bakshi Nagar, Jammu, Jammu and Kashmir 180001", "+911912584234", 32.736122,74.854392));
        hospitalArrayList.add(new ItemHospital("S.H.S Memorial Hospital & Maternity Centre", "33, Rehari Chungi, Gurudwara Road Jammu Jammu and Kashmir, 180005", "+911912580754", 32.743334,74.858946));
        hospitalArrayList.add(new ItemHospital("MAHARISHI DAYANAND HOSPITAL & MEDICAL RESEARCH CENTRE", "PVG3+6G9, Nawabad, Jammu, Jammu and Kashmir 180016", "+911912545225", 32.726417,74.854531));
        hospitalArrayList.add(new ItemHospital("Govt. Hospital Sarwal", "Rehari Colony, Jammu, Jammu and Kashmir 180005", "+911912579402", 32.747124,32.747124));
        hospitalArrayList.add(new ItemHospital("Swami Vivekananda Hospital", "Ved Mandir Road, Amphalla, Jammu, Jammu and Kashmir 180005", "+911912547418", 32.745179,74.865247));
        hospitalArrayList.add(new ItemHospital("Government Hospital Gandhi Nagar, Jammu", "Gandhi Nagar, Jammu, Jammu and Kashmir 180004", "+911912430041", 32.705234,74.859240));
        hospitalArrayList.add(new ItemHospital("Narayana Hospital", "15-C, Sansar Tower, 2nd Extension, Gandhi Nagar", "NA", 32.708982,74.870513));
        hospitalArrayList.add(new ItemHospital("Shri Mata Vaishno Devi Narayana Superspeciality Hospital", "Kakryal (Village & Post) Katra Tehsil Reasi District, Katra, Jammu and Kashmir 182320", "+919070888111", 32.946867,74.947600));
        hospitalArrayList.add(new ItemHospital("Satyam Hospital", "64, 65/A-4, Dr KN Katju Marg, Pocket 4, Sector 16A, Rohini, Delhi, 110089", "NA", 28.732450,77.125360));
        hospitalArrayList.add(new ItemHospital("Rathee hospital", "gate no.2, A1/159-160, opposite japanese park, Sector 11, Rohini, New Delhi, Delhi 110085", "+919540055690", 28.727738,77.114823));
        hospitalArrayList.add(new ItemHospital("Max Super Speciality Hospital, Shalimar Bagh", "FC 50, Max Wali Rd, C and D Block, Shalimar Place Site, Shalimar Bagh, New Delhi, Delhi 110088", "+911166422222", 28.728161,77.152814));
        hospitalArrayList.add(new ItemHospital("Dr. Baba Saheb Ambedkar Hospital", "Metro Station, Bhagawan Mahavir Marg, Sector 6 Rd, Rohini, New Delhi, Delhi 110085", "+911127933258", 28.711459,77.107638));
        hospitalArrayList.add(new ItemHospital("ESI Hospital", "Dr KN Katju Marg, Opp HP Petrol Pump, Sector 15, Sector 15A, Rohini, New Delhi, Delhi 110089", "+911127861033", 28.727538,77.126051));
        hospitalArrayList.add(new ItemHospital("Deep Chand Bandhu Hospital", "Ashok Vihar Rd, Kokiwala Bagh, Phase 4, Ashok Vihar IV, Ashok Vihar, New Delhi, Delhi 110052", "+911127305950", 28.681257,77.177664));
        hospitalArrayList.add(new ItemHospital("Bhagwan Mahavir Hospital", "H-4,5, Pitam Pura, Delhi, 110034", "+911127034535", 28.688468,77.117413));
        hospitalArrayList.add(new ItemHospital("Jag Pravesh Chandra Hospital", "Panduk Shila Marg, near Northern Engineering College, Shastri Park, Shahdara, New Delhi, Delhi 110053", "+911122184453", 28.676171,77.262722));
        hospitalArrayList.add(new ItemHospital("Govind Ballabh Pant Hospital", "1, Jawaharlal Nehru Marg, 64 Khamba, Raj Ghat, New Delhi, Delhi 110002", "+911123234242", 28.639158,77.235350));
        hospitalArrayList.add(new ItemHospital("Maharishi Valmiki Hospital", "Government of, Puth Khurd Village, New Delhi, Delhi 110039", "+911127761519", 28.775383,77.047683));
        hospitalArrayList.add(new ItemHospital("Pt. Madan Mohan Malaviya Hospital", "Khirki Extension, Malviya Nagar, New Delhi, Delhi 110017", "+911126689999", 28.535051,77.213623));
        hospitalArrayList.add(new ItemHospital("Lok Nayak Hospital", "metro Station Central, Jawaharlal Nehru Marg, near Delhi Gate, New Delhi, Delhi 110002", "+911123232400", 28.638222,77.238511));
        hospitalArrayList.add(new ItemHospital("Sardar Vallabh Bhai Patel Hospital", "East Patel Nagar, Patel Nagar, New Delhi, Delhi 110008", "+911125881201", 28.647209,77.168773));
        hospitalArrayList.add(new ItemHospital("AIIMS Hospital - New Delhi", "AIIMS Campus Temple, Hospital Store, Ansari Nagar East, New Delhi, Delhi 110016", "NA", 28.567429,77.209493));

        String latFetch = getItemFromSharedPreference("latitude");
        if(latFetch.isEmpty()) latFetch = "28.736896";

        String longFetch = getItemFromSharedPreference("longitude");
        if(longFetch.isEmpty()) longFetch = "77.111996";

        double latitude = Double.parseDouble(latFetch);
        double longitude = Double.parseDouble(longFetch);

        ItemHospital closest = hospitalArrayList.get(0);
        double diffLat = closest.Latitude - latitude;
        double diffLong = closest.Longitude - longitude;

        for(int i =1; i < hospitalArrayList.size(); i++){
            ItemHospital hospital = hospitalArrayList.get(i);

            if(latitude - hospital.Latitude < diffLat && longitude - hospital.Longitude < diffLong){
                closest = hospital;
                diffLat = latitude - hospital.Latitude;
                diffLong = longitude - hospital.Longitude;
            }
        }

        return closest;
    }


//    "Name" : "Sant Soham hospital",
//            "Address" : "Rithala Road, Pocket 4, Sector 11E, Rohini, Delhi, 110085",
//            "MobileNumber" : "NA",
//            "Latitude" : "28.735617",
//            "Longitude": "77.112993"

    static class ItemHospital{
        String Name, Address, MobileNumber;
        double Latitude, Longitude;
        public ItemHospital(String name, String address, String mobileNumber, double latitude, double longitude) {
            Name = name;
            Address = address;
            MobileNumber = mobileNumber;
            Latitude = latitude;
            Longitude = longitude;
        }

        public ItemHospital() {
        }
    }
}