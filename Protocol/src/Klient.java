import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Klient implements Runnable {

    private int id;
    private Socket clientsocket;
    private DataInputStream in;
    private DataOutputStream out;
    private Serwer ser;
    private byte[] pakiet = new byte[4];
    private boolean warunek = true;

    Klient(ServerSocket ssocket, int clientid, Serwer s) {
        try {

            //oczekiwanie na podłączenie klienta
            clientsocket = ssocket.accept();

            //stworzenie obiektów do obwługi wejścia/wyjścia
            in = new DataInputStream(clientsocket.getInputStream());
            out = new DataOutputStream(clientsocket.getOutputStream());

            //przypisanie id klienta,który będzie zawarty w odpowiedziach serwera
            id = clientid;

            //przekazanie referencji do obiektu serwera - wykorzystanie funkcji sprawdzających
            ser = s;

            //wysłanie klientowi jego id i potwierdzenie połączenia
            wyslijpakiet(0, 0, 0, 0);
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private byte[] generujPakiet(int operacja, int odpowiedz, int liczba, int czas) {
        /*

            operacja - 3 bity
            odpowiedź - 3 bity
            identyfiaktor - 5 bitów
            liczba - 8 bitów
            czas - 8 bitów
            dopełnienie - 5 bitów (zera)

            000|000|00   000|00000   000|00000   000|00000
            OP |ODP|   ID   |     L     |    TIME   | DOP

            000|000|00
            000|00000
            000|00000
            000|00000

         */

        byte[] ret = new byte[4];   //tworzenie nowej, pustej tablicy czterobajtowej

        //zapisanie 3 najmłodszych bitów zmiennej operacja na pole operacji
        ret[0] = (byte) ((operacja & 0b00000111) << 5);

        //zapisanie 3 najmłodszych bitów zmiennej odpowiedź na polu odpowiedzi
        ret[0] = (byte) (ret[0] | (byte) ((odpowiedz & 0b00000111) << 2));

        //zapisanie 2 starszych bitów zmiennej id na najmłodszych bitach pierwszego bajtu danych
        ret[0] = (byte) (ret[0] | (byte) ((id & 0b00011000) >> 3));

        //zapisanie 3 najmłodszych bitów zmiennej id na najstarszych bitach drugiego bajtu danych
        ret[1] = (byte) ((id & 0b00000111) << 5);

        //zapisanie 5 najstarszych bitów zmiennej liczba na najmłodszych bitach drugiego bajtu danych
        ret[1] = (byte) (ret[1] | (byte) ((liczba & 0b11111000) >> 3));

        //zapisanie 3 pozostałych bitów zmiennej liczba na najstarszych bitach trzeciego bajtu danych
        ret[2] = (byte) ((liczba & 0b00000111) << 5);

        //zapisanie 5 najstarszych bitów zmiennej czas na najmłodszych bitach trzeciego bajtu danych
        ret[2] = (byte) (ret[2] | (byte) ((czas & 0b11111000) >> 3));

        //zapisanie 3 pozostałych bitów na najstarszych bitach czwartego bajtu danych
        ret[3] = (byte) ((czas & 0b00000111) << 5);

        System.out.println("Wysłano:");
        for(int i = 0;i<ret.length;i++){
            String s1 = String.format("%8s", Integer.toBinaryString(ret[i] & 0xFF)).replace(' ', '0');
            System.out.print(s1+" ");
        }
        System.out.print("\n do klienta "+id+"\n");
        return ret;
    }

    void wyslijpakiet(int operacja, int odpowiedz, int liczba, int czas) {
        try {
            //wysłanie na gniazdo klienta wygenerowanego pakietu danych o długości 4 bajtów na podstawie wartości zmiennych
            out.write(generujPakiet(operacja, odpowiedz, liczba, czas), 0, 4);
        } catch (IOException r) {
            System.err.println(r.getMessage());
        }
    }

    private void zakoncz() {
        try {
            //zamknięcie gniazda klienta po zakończeniu działania programu
            clientsocket.close();
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
        } finally {
            //ustawienie warunku zakończenia pętli nasłuchującej
            warunek = false;
        }
    }

    private void execute(int operacja, int odpowiedz, int liczba) {

        //wykonanie akcji na podstawie komunikatów otrzymanych od klienta

        //operacja 011 i odpowiedź 000 -> sprawdzenie zgadywanej wartości
        if (operacja == 3 && odpowiedz == 0) {
            ser.sprawdz(liczba, this);
        }

        //operacja 111 i odpowiedź 111 -> powiadomienie od klienta o zakończeniu połączenia
        if (operacja == 7 && odpowiedz == 7) {
            System.out.println("Klient " + id + " kończy połączenie");
            zakoncz();
        }
    }

    private void decode(byte[] data) {

        //funkcja pobiera odpowiednie wartości z pól danych komunikatu

        int odpowiedz, sesja, operacja, liczba;

        //pobranie kodu operacji z pierwszego bajtu
        operacja = (data[0] & 0b11100000) >> 5;

        //pobranie kodu odpowiedzi z pierwszego bajtu danych
        odpowiedz = (data[0] & 0b00011100) >> 2;

        //pobranie id sesji z pierwszego i drugiego bajtu danych
        sesja = ((data[0] & 0b00000011) << 3) | ((data[1] & 0b11100000) >> 5);

        //pobranie liczby z drugiego i trzeciego bajtu danych
        liczba = ((data[1] & 0b00011111) << 3) | ((data[2] & 0b11100000) >> 5);

        //pole czas jest ignorowane, ponieważ klient nigdy nie wysyła danych czasu do serwera

        //uwarunkowanie poprawności pakietu numerem id
        if (sesja == id) {
            execute(operacja, odpowiedz, liczba);
        } else {
            System.out.println("Odebrano niepoprawny komunikat od klienta " + id);
        }
    }

    public void run() {
        int len;
        while (warunek) {
            try {
                //oczekiwanie na komunikat od klienta
                len = in.read(pakiet);

                //w przypadku przerwania połączenia, funkcja read zwraca wartość -1
                //związane jest to ze strumieniowym wysyłaniem danych w TCP
                if (len == -1) {
                    System.out.println("Klient " + id + " rozłączył się");
                    warunek = false;
                    break;
                }
                //odczytanie komunikatu, jeśli nie ma nieprawidłowości
                else decode(pakiet);
            } catch (java.io.IOException e) {
                System.err.println(e.getMessage());
            }
        }


    }

}
