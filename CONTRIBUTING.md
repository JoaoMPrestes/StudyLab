# Contribuindo para o StudyLab

Obrigado por seu interesse em contribuir para o **StudyLab**!  
Este documento explica como preparar o ambiente, enviar melhorias, reportar bugs e participar do desenvolvimento.

---

## 📌 Regras Gerais

- Seja claro e objetivo nos commits.
- Descreva bem seus Pull Requests.
- Nunca envie código quebrado.
- Mantenha sempre a branch `main` estável.
- Leia o **README.md** antes de começar.

---

## 🛠️ Preparando o ambiente de desenvolvimento

1. Faça o fork do repositório.
2. Clone o seu fork:
   ```bash
   git clone https://github.com/SEU_USUARIO/StudyLab.git
3.Entre na pasta:
	cd StudyLab
4.Garanta que possui:
- Java 21+
- Maven 3.9+
- JavaFX configurado

## 🌿 Fluxo de trabalho (Git Workflow):

1. Crie uma nova branch para cada mudança:
	git checkout -b feature/nome-da-feature
	# ou
	git checkout -b fix/nome-do-bug
2. Faça commits pequenos e organizados:
	git add .
	git commit -m "feat: descrição clara da mudança"
3. Envie a branch:
	git push origin feature/nome-da-feature
4. Abra um Pull Request no GitHub
No PR:
- Explique o que mudou
- Como testar
- Se a mudança quebra algo
- Prints, se possível

## ✨ Padrão de Commits (Conventional Commits)
Use sempre:

#########################################################
| Tipo	   | Quando usar				|
|-------------------------------------------------------|
| feat:	   | nova funcionalidade			|
| fix:	   | correção de bug				|
| docs:	   | documentação				|
| style:   | formatação, sem mudanças de lógica		|
| refactor | melhoria interna sem alterar comportamento |
| test:	   | testes automatizados			|
| chore:   | tarefas internas, build, dependências	|
#########################################################

Exemplos:

feat: add screen transition animation
fix: JSON file not loading correctly
docs: update README installation section

## 🐛 Reportando Problemas (Issues)

Antes de abrir uma issue:

1.Verifique se o problema já foi reportado.

2.Inclua:

- Passo a passo para reproduzir
- Print da tela
- Sistema operacional e versão da JDK
- Logs de erro

## 🔧 Padrões de Código:
- Organize sempre os imports.
- Evite duplicação de código.
- Métodos claros, curtos e com nomes descritivos.
- Nunca deixe "System.out.println" em versão final — use logs.

## 📦 Estrutura de Pastas

src/main/java      → código principal
src/main/resources → FXML, CSS, ícones
src/test           → testes futuros
dist               → futuras releases

## 🤝 Código de Conduta
Ao contribuir, você concorda em manter um ambiente respeitoso para todos.

## ✔️ Contribuições são bem-vindas!

Pull Requests, Issues, ideias, críticas construtivas — tudo é bem-vindo.
Obrigado por ajudar o StudyLab a crescer! 🚀





