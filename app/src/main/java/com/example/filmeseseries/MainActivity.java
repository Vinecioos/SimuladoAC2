package com.example.filmeseseries;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText editNome, editAno, editNota, editBusca;
    private Spinner spinnerTipo, spinnerGenero, spinnerFiltro;
    private CheckBox checkAssistiu;
    private Button btnSalvar, btnLimpar;
    private ListView listaObras;
    private TextView textVazio;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private final List<Obra> todas = new ArrayList<>();
    private final List<Obra> exibidas = new ArrayList<>();
    private ArrayAdapter<Obra> adapter;
    private String idEdicao = null;

    private final String[] tipos = {"Selecione o tipo", "Filme", "Série", "Documentário", "Animação"};
    private final String[] generos = {"Selecione o gênero", "Ação", "Comédia", "Drama", "Terror", "Ficção Científica", "Romance", "Fantasia", "Suspense", "Aventura", "Outro"};
    private final String[] filtros = {"Todos os tipos", "Filme", "Série", "Documentário", "Animação"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editNome = findViewById(R.id.editNome);
        editAno = findViewById(R.id.editAno);
        editNota = findViewById(R.id.editNota);
        editBusca = findViewById(R.id.editBusca);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        spinnerGenero = findViewById(R.id.spinnerGenero);
        spinnerFiltro = findViewById(R.id.spinnerFiltro);
        checkAssistiu = findViewById(R.id.checkAssistiu);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnLimpar = findViewById(R.id.btnLimpar);
        listaObras = findViewById(R.id.listaObras);
        textVazio = findViewById(R.id.textVazio);

        db = FirebaseFirestore.getInstance();
        configurarSpinner(spinnerTipo, tipos);
        configurarSpinner(spinnerGenero, generos);
        configurarSpinner(spinnerFiltro, filtros);

        adapter = new ArrayAdapter<Obra>(this, android.R.layout.simple_list_item_2, android.R.id.text1, exibidas) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView titulo = view.findViewById(android.R.id.text1);
                TextView subtitulo = view.findViewById(android.R.id.text2);
                Obra o = getItem(position);
                if (o != null) {
                    titulo.setText(o.getNome());
                    titulo.setTextColor(0xFF24213A);
                    titulo.setTextSize(16);
                    subtitulo.setText(o.getTipo() + " • " + o.getGenero() + " • " + o.getAno()
                            + " • Nota: " + String.format(Locale.getDefault(), "%.1f", o.getNota())
                            + (o.isAssistiu() ? " • Assistido" : " • Não assistido"));
                    subtitulo.setTextColor(0xFF686579);
                    subtitulo.setTextSize(12);
                }
                return view;
            }
        };
        listaObras.setAdapter(adapter);

        btnSalvar.setOnClickListener(v -> salvarObra());
        btnLimpar.setOnClickListener(v -> limparFormulario());
        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { aplicarFiltros(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        editBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { aplicarFiltros(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        listaObras.setOnItemClickListener((parent, view, position, id) -> carregarParaEdicao(exibidas.get(position)));
        listaObras.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmarExclusao(exibidas.get(position));
            return true;
        });
        ouvirFirestore();
    }

    private void configurarSpinner(Spinner spinner, String[] opcoes) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcoes);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    private void salvarObra() {
        String nome = editNome.getText().toString().trim();
        String anoTexto = editAno.getText().toString().trim();
        String notaTexto = editNota.getText().toString().trim().replace(',', '.');

        if (nome.isEmpty()) { editNome.setError("Informe o nome"); editNome.requestFocus(); return; }
        if (spinnerTipo.getSelectedItemPosition() == 0) { aviso("Selecione o tipo."); return; }
        if (spinnerGenero.getSelectedItemPosition() == 0) { aviso("Selecione o gênero."); return; }
        if (anoTexto.isEmpty()) { editAno.setError("Informe o ano"); editAno.requestFocus(); return; }
        if (notaTexto.isEmpty()) { editNota.setError("Informe sua nota"); editNota.requestFocus(); return; }

        int ano;
        double nota;
        try {
            ano = Integer.parseInt(anoTexto);
            if (ano < 1888 || ano > 2100) { editAno.setError("Informe um ano entre 1888 e 2100"); return; }
        } catch (NumberFormatException e) { editAno.setError("Ano inválido"); return; }
        try {
            nota = Double.parseDouble(notaTexto);
            if (nota < 0 || nota > 10) { editNota.setError("A nota deve ser de 0 a 10"); return; }
        } catch (NumberFormatException e) { editNota.setError("Nota inválida"); return; }

        Obra obra = new Obra(nome, spinnerTipo.getSelectedItem().toString(),
                spinnerGenero.getSelectedItem().toString(), ano, nota, checkAssistiu.isChecked());
        java.util.Map<String, Object> dados = new java.util.HashMap<>();
        dados.put("nome", obra.getNome());
        dados.put("tipo", obra.getTipo());
        dados.put("genero", obra.getGenero());
        dados.put("ano", obra.getAno());
        dados.put("nota", obra.getNota());
        dados.put("assistiu", obra.isAssistiu());

        btnSalvar.setEnabled(false);
        if (idEdicao == null) {
            db.collection("obras").add(dados)
                    .addOnSuccessListener(ref -> { aviso("Obra cadastrada no Firebase!"); limparFormulario(); })
                    .addOnFailureListener(e -> aviso("Erro ao salvar: " + e.getMessage()))
                    .addOnCompleteListener(task -> btnSalvar.setEnabled(true));
        } else {
            String id = idEdicao;
            db.collection("obras").document(id).set(dados)
                    .addOnSuccessListener(unused -> { aviso("Obra atualizada!"); limparFormulario(); })
                    .addOnFailureListener(e -> aviso("Erro ao atualizar: " + e.getMessage()))
                    .addOnCompleteListener(task -> btnSalvar.setEnabled(true));
        }
    }

    private void ouvirFirestore() {
        listener = db.collection("obras").addSnapshotListener((snapshots, error) -> {
            if (error != null) { textVazio.setText("Não foi possível carregar os dados. Confira a conexão e o Firebase."); return; }
            if (snapshots == null) return;
            todas.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Obra o = new Obra(
                        doc.getString("nome") == null ? "" : doc.getString("nome"),
                        doc.getString("tipo") == null ? "" : doc.getString("tipo"),
                        doc.getString("genero") == null ? "" : doc.getString("genero"),
                        doc.getLong("ano") == null ? 0 : doc.getLong("ano").intValue(),
                        doc.getDouble("nota") == null ? 0.0 : doc.getDouble("nota"),
                        Boolean.TRUE.equals(doc.getBoolean("assistiu")));
                o.setId(doc.getId());
                todas.add(o);
            }
            aplicarFiltros();
        });
    }

    private void aplicarFiltros() {
        if (adapter == null) return;
        String tipo = spinnerFiltro.getSelectedItem() == null ? filtros[0] : spinnerFiltro.getSelectedItem().toString();
        String busca = editBusca.getText().toString().trim().toLowerCase(Locale.ROOT);
        exibidas.clear();
        for (Obra o : todas) {
            boolean tipoOk = tipo.equals("Todos os tipos") || tipo.equals(o.getTipo());
            boolean nomeOk = o.getNome().toLowerCase(Locale.ROOT).contains(busca);
            if (tipoOk && nomeOk) exibidas.add(o);
        }
        adapter.notifyDataSetChanged();
        textVazio.setText(exibidas.isEmpty() ? "Nenhuma obra encontrada." : "Toque para editar • segure para excluir");
        textVazio.setVisibility(exibidas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void carregarParaEdicao(Obra o) {
        idEdicao = o.getId();
        editNome.setText(o.getNome());
        selecionar(spinnerTipo, o.getTipo());
        selecionar(spinnerGenero, o.getGenero());
        editAno.setText(String.valueOf(o.getAno()));
        editNota.setText(String.valueOf(o.getNota()));
        checkAssistiu.setChecked(o.isAssistiu());
        btnSalvar.setText("Atualizar");
        Toast.makeText(this, "Editando: " + o.getNome(), Toast.LENGTH_SHORT).show();
    }

    private void selecionar(Spinner spinner, String valor) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(valor)) { spinner.setSelection(i); return; }
        }
    }

    private void confirmarExclusao(Obra o) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir obra?")
                .setMessage("Deseja excluir "" + o.getNome() + ""? Essa ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Excluir", (dialog, which) ->
                        db.collection("obras").document(o.getId()).delete()
                                .addOnSuccessListener(unused -> {
                                    aviso("Obra excluída.");
                                    if (o.getId().equals(idEdicao)) limparFormulario();
                                })
                                .addOnFailureListener(e -> aviso("Erro ao excluir: " + e.getMessage())))
                .show();
    }

    private void limparFormulario() {
        idEdicao = null;
        editNome.setText("");
        editAno.setText("");
        editNota.setText("");
        spinnerTipo.setSelection(0);
        spinnerGenero.setSelection(0);
        checkAssistiu.setChecked(false);
        btnSalvar.setText("Salvar");
        editNome.setError(null);
        editAno.setError(null);
        editNota.setError(null);
    }

    private void aviso(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
