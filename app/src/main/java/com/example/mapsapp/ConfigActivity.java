package com.example.mapsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ConfigActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private EditText inputPeso;
    private EditText inputAltura;
    private RadioGroup radioGenero;
    private EditText inputNascimento;
    private RadioGroup radioMap;
    private RadioGroup radioNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        setTitle("Configurações");

        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPreferences.edit();

        inputPeso = findViewById(R.id.input_peso);
        inputAltura = findViewById(R.id.input_altura);
        radioGenero = findViewById(R.id.RadioGroupSex);
        inputNascimento = findViewById(R.id.input_birth);

        radioMap = findViewById(R.id.RadioGroupMap);

        radioNavigation = findViewById(R.id.RadioGroupNavigation);

        String savedPeso = sharedPreferences.getString("peso_salvo", "");
        String savedAltura = sharedPreferences.getString("altura_salvo", "");
        String savedNascimento = sharedPreferences.getString("nascimento_salvo", "");

        inputPeso.setText(savedPeso);
        inputAltura.setText(savedAltura);
        inputNascimento.setText(savedNascimento);

        int generoIdSalvo = sharedPreferences.getInt("genero_selecionado", -1);
        if (generoIdSalvo != -1 && radioGenero != null) {
            radioGenero.check(generoIdSalvo);
        }

        int mapaIdSalvo = sharedPreferences.getInt("mapa_selecionado", -1);
        if (mapaIdSalvo != -1 && radioMap != null) {
            radioMap.check(mapaIdSalvo);
        }

        int navegacaoIdSalva = sharedPreferences.getInt("navegacao_selecionada", -1);
        if (navegacaoIdSalva != -1 && radioNavigation != null) {
            radioNavigation.check(navegacaoIdSalva);
        }



        if (radioGenero != null) {
            radioGenero.setOnCheckedChangeListener((group, checkedId) -> {
                e.putInt("genero_selecionado", checkedId);
                e.apply();
            });
        }

        if (radioMap != null) {
            radioMap.setOnCheckedChangeListener((group, checkedId) -> {
                e.putInt("mapa_selecionado", checkedId);
                e.apply();
            });
        }

        if (radioNavigation != null) {
            radioNavigation.setOnCheckedChangeListener((group, checkedId) -> {
                e.putInt("navegacao_selecionada", checkedId);
                e.apply();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor e = sharedPreferences.edit();

        e.putString("peso_salvo", inputPeso.getText().toString());
        e.putString("altura_salvo", inputAltura.getText().toString());
        e.putString("nascimento_salvo", inputNascimento.getText().toString());

        e.apply();
    }
}