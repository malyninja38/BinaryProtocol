import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client implements Runnable {

    private int idsesji;
    private Socket socket;
    private DataInputStream sin;
    private DataOutputStream sout;
    private static boolean cond = true;
    private boolean ingame = false;

    private Client(String inet, int port) {
        try {
            System.out.println("Oczekiwanie na połączenie...");

            //stworzenie gniazda i nawiązanie połączenia z serwerem
            socket = new Socket(inet, port);

            //stworzenie obiektów obsługujących wejście/wyjście
            sin = new DataInputStream(socket.getInputStream());
            sout = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private void execute(int operacja, int odpowiedz, int liczba, int czas, int sesja) {

        //reakcje na komunikaty serwera
        switch (operacja) {
            case 2:
                if (odpowiedz == 0) {
                    System.out.println("Start!");
                    ingame = true;
                }
                break;
            case 3:
                if (ingame) {
                    if (odpowiedz == 1) {
                        System.out.println("Liczba jest za mała");
                    }
                    if (odpowiedz == 2) {
                        System.out.println("Pozostało " + czas + " sekund");
                    }
                    if (odpowiedz == 4){
                        System.out.println("Liczba jest za duża");
                    }
                }
                break;
            case 7:

                //niezależnie od odpowiedzi serwera, otrzymanie komunikatu z operacją zakończenia
                //powoduje zamknięcie połączenia i zakończenie pracy
                if (odpowiedz == 0) {
                    System.out.println("Wygrał drugi gracz, poprawna liczba to: " + liczba);

                }
                if (odpowiedz == 1) {
                    System.out.println("Wygrałeś!");
                }
                if (odpowiedz == 2) {
                    System.out.println("Czas się skończył.");
                }

                //powiadomienie serwera o rozłączeniu
                send(0, 7, 7, idsesji);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                //ustawienie warunku zakońćzenia pętli gry
                ingame = cond = false;
                break;
            default:
                break;
        }
    }

    private void decode(byte[] data) {


        //algorytm odczytu danych jest identyczny jak w serwerze
        int odpowiedz, sesja, operacja, liczba, czas;

        operacja = (data[0] & 0b11100000) >> 5;
        odpowiedz = (data[0] & 0b00011100) >> 2;
        sesja = ((data[0] & 0b00000011) << 3) | ((data[1] & 0b11100000) >> 5);
        liczba = ((data[1] & 0b00011111) << 3) | ((data[2] & 0b11100000) >> 5);
        czas = ((data[2] & 0b00011111) << 3) | ((data[3] & 0b11100000) >> 5);

        if (sesja == idsesji) {
            execute(operacja, odpowiedz, liczba, czas, sesja);
        } else {
            if (operacja == 0 && odpowiedz == 0) {

                //osobna reakcja na otrzymanie id, ponieważ w tym momencie klient nie wie jakie id dostanie
                //więc sprawdzanie poprawności komunikatu zakończy się niepowodzeniem
                this.idsesji = sesja;
                System.out.println("Otrzymano ID "+idsesji);
            } else
                System.out.println("Odebrano niepoprawny komunikat od serwera");
        }

    }

    private byte[] generujPakiet(int operacja, int odpowiedz, int id, int liczba) {

        //algorytm generowania komunikatu jest identyczna jak w serwerze
        byte[] packet = new byte[4];

        packet[0] = (byte) ((operacja & 0b00000111) << 5);
        packet[0] = (byte) (packet[0] | (byte) ((odpowiedz & 0b00000111) << 2));
        packet[0] = (byte) (packet[0] | (byte) ((id & 0b00011000) >> 3));

        packet[1] = (byte) ((id & 0b00000111) << 5);
        packet[1] = (byte) (packet[1] | (byte) ((liczba & 0b11111000) >> 3));

        packet[2] = (byte) ((liczba & 0b00000111) << 5);

        System.out.println("Wysłano:");
        for(int i = 0;i<packet.length;i++){
            String s1 = String.format("%8s", Integer.toBinaryString(packet[i] & 0xFF)).replace(' ', '0');
            System.out.print(s1+" ");
        }
        System.out.print("\n do klienta "+id+"\n");

        return packet;
    }

    private void send(int liczba, int operacja, int odpowiedz, int id) {
        try {

            //wysłanie komunikatu wygenerowanego na podstawie wartości zmiennych
            sout.write(generujPakiet(operacja, odpowiedz, id, liczba), 0, 4);
        } catch (IOException r) {
            System.err.println(r.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length == 2) {

            //stworzenie obiektu klienta
            Client client = new Client(args[0], Integer.parseInt(args[1]));
            if (client.socket != null) {

                //reakcja na otrzymanie połączenia
                System.out.println("Połączono z serwerem " + args[0] + ":" + args[1]);

                //uruchomienie wątku z pętlą gry
                new Thread(client).start();
            } else {

                //reakcja na brak połączenia z serwerem
                System.out.println("Nie można było połączyć z serwerem");
                cond = false;
            }
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        int liczba, len;
        byte[] data = new byte[4];
        while (cond) {
            try {

                //sprawdzenie czy użytkownik wpisał wartość
                if (System.in.available() > 0) {
                    liczba = scanner.nextInt();

                    //wysłanie wpisanej wartości
                    send(liczba, 3, 0, idsesji);
                }
            } catch (Throwable e) {

                //ignorowanie danych niemożliwych do konwersji na liczbę
                scanner.next();
            }
            try {

                //sprawdzenie czy na wejściu gniazda pojawił się komunikat
                if (sin.available() > 0) {

                    //odczytanie komunikatu
                    len = sin.read(data);

                    if (len == -1) {

                        //zakończenie pracy klienta w momencie zerwania połączenia
                        cond = false;
                    } else {

                        //odczytanie komunikatu, który przyszedł od serwera
                        decode(data);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        //zamknięcie gniazda po zakończeniu pętli gry
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }


    }

}
