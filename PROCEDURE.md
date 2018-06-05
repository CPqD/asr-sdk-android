# Configuração do projeto

A biblioteca de reconhecimento de fala é fornecida no formato AAR.

## Importando a biblioteca no Android Studio

Para importar a biblioteca, é necessário fazer os seguintes passos no projeto do aplicativo:
	- Na barra de menu, navegar em File > New > New Module…
	- Import .JAR/.AAR Package > Next
	- Selecione o arquivo .AAR correspondente à biblioteca. Será dado um nome ao projeto automaticamente. Pressione o botão Finish.

## Ajustando o script de build

Após importar a biblioteca no projeto do aplicativo, o script de build (build.gradle do módulo do aplicativo) deve ser alterado para incorporar as novas dependências, no caso, a CPqD ASR Recognizer e o Tyrus, biblioteca de WebSocket.

	dependencies { 
		// ... 
		
		// CPqD ASR Recognizer. 
		compile project(':recognizer') 
		
		// Dependência de biblioteca de WebSocket. 
		compile 'org.glassfish.tyrus:tyrus-container-grizzly-client:1.13' 
	}