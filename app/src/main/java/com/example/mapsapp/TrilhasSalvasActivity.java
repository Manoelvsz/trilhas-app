package com.example.mapsapp;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TrilhasSalvasActivity extends AppCompatActivity {

    private TrilhaDBHelper dbHelper;
    private SQLiteDatabase database;
    private ListView listViewTrilhas;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trilhas_salvas);

        dbHelper = new TrilhaDBHelper(this);
        database = dbHelper.getReadableDatabase();
        listViewTrilhas = findViewById(R.id.listview_trilhas);

        listViewTrilhas.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(TrilhasSalvasActivity.this, DetalhesTrilhaActivity.class);
            intent.putExtra("TRILHA_ID", id);
            startActivity(intent);
        });

        listViewTrilhas.setOnItemLongClickListener((parent, view, position, id) -> {
            showOptionsDialog(id);
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Apagar Todas");
        menu.add(0, 2, 0, "Apagar por Data");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                confirmarApagarTodas();
                return true;
            case 2:
                mostrarDialogoData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showOptionsDialog(long trilhaId) {
        String[] options = {"Editar", "Excluir", "Compartilhar"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opções da Trilha");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showEditDialog(trilhaId);
                    break;
                case 1:
                    confirmarExcluirTrilha(trilhaId);
                    break;
                case 2:
                    compartilharTrilha(trilhaId);
                    break;
            }
        });
        builder.show();
    }

    private void confirmarExcluirTrilha(long trilhaId) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Trilha")
                .setMessage("Tem certeza que deseja excluir esta trilha?")
                .setPositiveButton("Sim", (dialog, which) -> excluirTrilha(trilhaId))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirTrilha(long trilhaId) {
        database.delete(TrilhaDBHelper.TABLE_DETALHES, TrilhaDBHelper.COLUMN_DETALHE_TRILHA_ID + "=?", new String[]{String.valueOf(trilhaId)});
        database.delete(TrilhaDBHelper.TABLE_TRILHAS, TrilhaDBHelper.COLUMN_TRILHA_ID + "=?", new String[]{String.valueOf(trilhaId)});
        carregarTrilhas();
        Toast.makeText(this, "Trilha excluída.", Toast.LENGTH_SHORT).show();
    }

    private void confirmarApagarTodas() {
        new AlertDialog.Builder(this)
                .setTitle("Apagar Todas as Trilhas")
                .setMessage("Tem certeza que deseja apagar TODAS as trilhas? Isso não pode ser desfeito.")
                .setPositiveButton("Sim", (dialog, which) -> {
                    database.delete(TrilhaDBHelper.TABLE_DETALHES, null, null);
                    database.delete(TrilhaDBHelper.TABLE_TRILHAS, null, null);
                    carregarTrilhas();
                    Toast.makeText(this, "Todas as trilhas foram apagadas.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void mostrarDialogoData() {
        // Simplificação: Apagar trilhas antes de uma data selecionada
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String dataSelecionada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    apagarTrilhasAntesDe(dataSelecionada);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.setTitle("Apagar trilhas ANTES de:");
        datePickerDialog.show();
    }

    private void apagarTrilhasAntesDe(String data) {
        // Assumindo formato YYYY-MM-DD no banco. O formato salvo é "yyyy-MM-dd HH:mm:ss"
        // Então podemos comparar strings
        String whereClause = TrilhaDBHelper.COLUMN_TRILHA_DATA + " < ?";
        String[] whereArgs = new String[]{data};
        
        // Primeiro precisamos pegar os IDs para apagar os detalhes
        Cursor cursor = database.query(TrilhaDBHelper.TABLE_TRILHAS, new String[]{TrilhaDBHelper.COLUMN_TRILHA_ID}, whereClause, whereArgs, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            database.delete(TrilhaDBHelper.TABLE_DETALHES, TrilhaDBHelper.COLUMN_DETALHE_TRILHA_ID + "=?", new String[]{String.valueOf(id)});
        }
        cursor.close();

        int rows = database.delete(TrilhaDBHelper.TABLE_TRILHAS, whereClause, whereArgs);
        carregarTrilhas();
        Toast.makeText(this, rows + " trilhas apagadas.", Toast.LENGTH_SHORT).show();
    }

    private void compartilharTrilha(long trilhaId) {
        Cursor cursor = database.rawQuery("SELECT t.*, d.* FROM " + TrilhaDBHelper.TABLE_TRILHAS + " t " +
                "JOIN " + TrilhaDBHelper.TABLE_DETALHES + " d ON t." + TrilhaDBHelper.COLUMN_TRILHA_ID + " = d." + TrilhaDBHelper.COLUMN_DETALHE_TRILHA_ID +
                " WHERE t." + TrilhaDBHelper.COLUMN_TRILHA_ID + " = ?", new String[]{String.valueOf(trilhaId)});

        if (cursor.moveToFirst()) {
            JsonObject json = new JsonObject();
            json.addProperty("nome", cursor.getString(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_TRILHA_NOME)));
            json.addProperty("data", cursor.getString(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_TRILHA_DATA)));
            json.addProperty("distancia", cursor.getFloat(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_DETALHE_DISTANCIA)));
            json.addProperty("tempo", cursor.getString(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_DETALHE_TEMPO)));
            json.addProperty("velocidade_max", cursor.getFloat(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_DETALHE_VELOCIDADE_MAX)));
            // Check if column exists before accessing to avoid crash on old DB versions if not upgraded properly
            int idxMedia = cursor.getColumnIndex(TrilhaDBHelper.COLUMN_DETALHE_VELOCIDADE_MEDIA);
            if (idxMedia != -1) {
                json.addProperty("velocidade_media", cursor.getFloat(idxMedia));
            }
            json.addProperty("calorias", cursor.getFloat(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_DETALHE_CALORIAS)));
            json.addProperty("path", cursor.getString(cursor.getColumnIndexOrThrow(TrilhaDBHelper.COLUMN_DETALHE_PATH)));

            String jsonString = new Gson().toJson(json);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, jsonString);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, "Compartilhar Trilha (JSON)");
            startActivity(shareIntent);
        }
        cursor.close();
    }

    private void showEditDialog(long trilhaId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Nome da Trilha");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Novo nome da trilha");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoNome = input.getText().toString();
            if (novoNome.isEmpty()) {
                Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show();
                return;
            }
            atualizarNomeTrilha(trilhaId, novoNome);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void atualizarNomeTrilha(long trilhaId, String novoNome) {
        ContentValues values = new ContentValues();
        values.put(TrilhaDBHelper.COLUMN_TRILHA_NOME, novoNome);

        int rowsAffected = database.update(TrilhaDBHelper.TABLE_TRILHAS, values, TrilhaDBHelper.COLUMN_TRILHA_ID + "=?", new String[]{String.valueOf(trilhaId)});

        if (rowsAffected > 0) {
            Toast.makeText(this, "Nome atualizado com sucesso!", Toast.LENGTH_SHORT).show();
            carregarTrilhas(); // Recarrega a lista para mostrar a alteração
        } else {
            Toast.makeText(this, "Erro ao atualizar o nome.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarTrilhas();
    }

    private void carregarTrilhas() {
        String[] from = {TrilhaDBHelper.COLUMN_TRILHA_NOME, TrilhaDBHelper.COLUMN_TRILHA_DATA};
        int[] to = {android.R.id.text1, android.R.id.text2};

        Cursor cursor = database.query(TrilhaDBHelper.TABLE_TRILHAS, 
            new String[]{TrilhaDBHelper.COLUMN_TRILHA_ID, TrilhaDBHelper.COLUMN_TRILHA_NOME, TrilhaDBHelper.COLUMN_TRILHA_DATA}, 
            null, null, null, null, TrilhaDBHelper.COLUMN_TRILHA_DATA + " DESC");

        adapter = new SimpleCursorAdapter(this, 
            android.R.layout.simple_list_item_2, 
            cursor, 
            from, 
            to, 
            0);

        listViewTrilhas.setAdapter(adapter);
    }
}