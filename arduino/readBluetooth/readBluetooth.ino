#include <SoftwareSerial.h>
#include <string.h>

//serial pins for bluetooth
const int BT_RX = 8;
const int BT_TX = 9;
const int BLUETOOTH_SSMAX = 60;
SoftwareSerial BluetoothSerial(BT_RX,BT_TX);
char hello[] = "Hello\n";

//serial pins for sonar1
#define echoPin1 5 //Pino 5 recebe o pulso do echo
#define trigPin1 6 //Pino 6 envia o pulso para gerar o echo
int distancia1=0;
SoftwareSerial Sonar1Serial(echoPin1,trigPin1);

//serial pins for sonar2
#define echoPin2 10 //Pino 5 recebe o pulso do echo
#define trigPin2 11 //Pino 6 envia o pulso para gerar o echo
int distancia2=0;
SoftwareSerial Sonar2Serial(echoPin2,trigPin2);


byte sendBuffer[BLUETOOTH_SSMAX]; //global buffer for outcoming data of bluetooth
byte receiveBuffer[BLUETOOTH_SSMAX]; //global buffer for incoming data of bluetooth

//delay
int READ_DELAY = 10; //10ms


//read distance from sonar 1
int readSonar_1(){
  //retorna distancia em centimetros
  //seta o pino 12 com um pulso baixo "LOW" ou desligado ou ainda 0
    digitalWrite(trigPin1, LOW);
  // delay de 2 microssegundos
    delayMicroseconds(2);
  //seta o pino 12 com pulso alto "HIGH" ou ligado ou ainda 1
    digitalWrite(trigPin1, HIGH);
  //delay de 10 microssegundos
    delayMicroseconds(10);
  //seta o pino 12 com pulso baixo novamente
    digitalWrite(trigPin1, LOW);
  //pulseInt lê o tempo entre a chamada e o pino entrar em high
    long duration = pulseIn(echoPin1,HIGH);
  //Esse calculo é baseado em s = v . t, lembrando que o tempo vem dobrado
  //porque é o tempo de ida e volta do ultrassom
    //long distancia = duration /29 / 2 ; //distancia em centimetros
    long distancia = (duration/3) / 2 ; //distancia em milimetros
    
    if(distancia > 1800)
        distancia = 1800;
        
    return (int)distancia;
}

//read distance from sonar 2
int readSonar_2(){
  //seta o pino 12 com um pulso baixo "LOW" ou desligado ou ainda 0
    digitalWrite(trigPin2, LOW);
  // delay de 2 microssegundos
    delayMicroseconds(2);
  //seta o pino 12 com pulso alto "HIGH" ou ligado ou ainda 1
    digitalWrite(trigPin2, HIGH);
  //delay de 10 microssegundos
    delayMicroseconds(10);
  //seta o pino 12 com pulso baixo novamente
    digitalWrite(trigPin2, LOW);
  //pulseInt lê o tempo entre a chamada e o pino entrar em high
    long duration = pulseIn(echoPin2,HIGH);
  //Esse calculo é baseado em s = v . t, lembrando que o tempo vem dobrado
  //porque é o tempo de ida e volta do ultrassom
    //long distancia = duration /29 / 2 ;
    long distancia = duration /3 / 2 ; //distancia em milimetros

    if(distancia > 1800)
        distancia = 1800;
        
    return (int)distancia;
}

//read from bluetooth serial at most length bytes.
int readBluetooth(byte * buffer, int length){

  int ptr;
  ptr=0;

  //read serial port.
  while((BluetoothSerial.available()) && (ptr < length)){
    buffer[ptr]=(byte)BluetoothSerial.read();
    ptr++;
  }

  
}

//write into bluetooth serial at most length bytes.
int writeBluetooth(byte * buffer, int length){

  int ptr;
  ptr=0;

  //read serial port.
  while((BluetoothSerial.available()) && (ptr < length)){
    BluetoothSerial.write(buffer[ptr]);
    ptr++;
  }

  
}

int setBuffer(byte * buffer, int length, byte value){
  int i=0;
  if(buffer == 0) return -1;
  for(i=0;i<length;i++){
      buffer[i] = value;
  }
  return 1;
}


void setup() {

  
  // Abre comunicacao serial e aguarda a abertura da porta
  Serial.begin(9600);

  //aguarda conexao da porta serial
  while (!Serial) {
    ; // wait for serial port to connect. Needed for Leonardo only
  }

  //Configura pinos a serem usados pelo modulo bluetooth
  pinMode(BT_RX,INPUT);  //BLUETOOTH_RX
  pinMode(BT_TX,OUTPUT); //BLUETOOTH_TX
  //Configura pinos a ser usado pelo sonar
  pinMode(echoPin1,INPUT);
  pinMode(trigPin1,OUTPUT); 

  //Abre comunicacao serial das portas seriais.
  Sonar1Serial.begin(9600);
  //Sonar2Serial.begin(9600);
  BluetoothSerial.begin(9600); // A ultima porta inicializada eh a que esta recebendo dados no momento.


}



void loop()
{

   //Initializes outcoming bluetoothBuffer
   setBuffer(sendBuffer,BLUETOOTH_SSMAX,(byte) 0);
  //Initializes incoming bluetoothBuffer
   setBuffer(receiveBuffer,BLUETOOTH_SSMAX,(byte) 0);
   distancia1=(readSonar_1() | 0xC000); //liga flag   
   distancia2=readSonar_2();
   //Read
   //readBluetooth(receiveBuffer,BLUETOOTH_SSMAX);   
   //Serial.println("\nValue received: ");
   //Serial.write(receiveBuffer,BLUETOOTH_SSMAX);
   
   //Write
   //writeBluetooth((byte*)hello,sizeof(hello));
   //Serial.println(hello);
   writeBluetooth((byte*)&distancia1,2);
   writeBluetooth((byte*)&distancia2,1);
   //                                                                                                                                                                                                        writeBluetooth((byte)distancia2,1);
   //Serial.println("\nValue sent: ");
   //Serial.println(hello);
   //Serial.println("Sonar 1:");
   //Serial.println(distancia1);
   Serial.println("Sonar 2:");
   Serial.println(distancia2);
   delay(READ_DELAY);
}



