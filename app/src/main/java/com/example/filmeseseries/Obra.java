package com.example.filmeseseries;

public class Obra {
    private String id, nome, tipo, genero;
    private int ano;
    private double nota;
    private boolean assistiu;

    public Obra() { }

    public Obra(String nome, String tipo, String genero, int ano, double nota, boolean assistiu) {
        this.nome = nome;
        this.tipo = tipo;
        this.genero = genero;
        this.ano = ano;
        this.nota = nota;
        this.assistiu = assistiu;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getGenero() { return genero; }
    public int getAno() { return ano; }
    public double getNota() { return nota; }
    public boolean isAssistiu() { return assistiu; }
}
