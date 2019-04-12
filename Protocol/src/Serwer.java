import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;


class Serwer {

    private int id1;
    private int id2;
    private int czasrozgrywki;
    private long poczatkowy;
    private int liczba;
    private boolean warunek = true;
    private ServerSocket socket;
    private Klient k1;
    private Klient k2;


    Serwer(int port) {
        try {

            //stworzenie gniazda nasłuchującego
            socket = new ServerSocket(port);
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void generuj() {
        Random generator = new Random();
        id1 = (generator.nextInt(30) + 1);
        id2 = (generator.nextInt(30) + 1);
    }

    private void maxczas() {

        //liczenie czasu rozgrywki z podanego wzoru
        czasrozgrywki = (Math.abs(id1 - id2) * 74) % 90 + 24;
    }

    private void losujliczbe() {
        Random generator = new Random();
        liczba = (generator.nextInt(254)+1);
        System.out.println("Wybrano " + liczba);
    }

    void sprawdz(int odp, Klient k) {

        //sprawdzenie odkodowanej liczby zgadywanej przez klienta z liczbą wylosowaną
        if (odp == liczba) {

            //wysłanie zgadującemu klientowi informacji o wygranej
            k.wyslijpakiet(7, 1, 0, 0);


            //wysłanie "temu drugiemu" informacji o przegranej
            if (k.equals(k1)) {
                k2.wyslijpakiet(7, 0, liczba, 0);
            } else {
                k1.wyslijpakiet(7, 0, liczba, 0);
            }
        } else {

            //wysłanie informacji klientowi o niepoprawnej liczbie
            if(odp < liczba)

                //liczba jest za mała
                k.wyslijpakiet(3, 1, 0, 0);
            else

                //liczba jest za duża
                k.wyslijpakiet(3, 4, 0, 0);
        }
    }

    private void ileczasu() {
        //pobranie aktualnego czasu w sekundach
        long obecny = System.currentTimeMillis() / 1000;

        //odjęcie aktualnego czasu od czasu startu gry
        long uplynelo = obecny - poczatkowy;

        //odjęcie czasu, który minął od maksymalnego czasu rozgrywki
        long zostalo = czasrozgrywki - uplynelo;

        if (zostalo > 0) {
            System.out.println("Zostalo " + zostalo + " sekund");

            //wysłanie informacji z pozostałym czasem do klientów
            k1.wyslijpakiet(3, 2, 0, (int) zostalo);
            k2.wyslijpakiet(3, 2, 0, (int) zostalo);
        } else {

            //wysłanie informacji o zakończeniu gry z powodu minięcia czasu
            k1.wyslijpakiet(7, 2, 0, 0);
            k2.wyslijpakiet(7, 2, 0, 0);

            //ustawienie warunku skończenia pętli gry
            warunek = false;
        }

    }

    void start() {
        //generowanie identyfikatorów dla dwóch klientów
        generuj();

        System.out.println("Oczekiwanie na klientów...");

        //tworzenie pierwszego obiektu nasłuchującego klienta i oczekiwanie na podłączenie
        k1 = new Klient(socket, id1, this);

        System.out.println("Połączono 1/2, id " + id1);

        //tworzenie pierwszego drugiego nasłuchującego klienta i oczekiwanie na podłączenie
        k2 = new Klient(socket, id2, this);
        System.out.println("Połączono 2/2, id " + id2);


        System.out.println("Przygotowywanie gry");
        maxczas();
        losujliczbe();
        poczatkowy = System.currentTimeMillis() / 1000;

        //zamknięcie gniazda dla większej ilości klientów (akceptowanie tylko dwóch)
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        //stworzenie wątków dla zasłuchiwania klientów
        Thread f1 = new Thread(k1);
        Thread f2 = new Thread(k2);

        //wysłanie klientom informacji o starcie gry
        k1.wyslijpakiet(2, 0, 0, 0);
        k2.wyslijpakiet(2, 0, 0, 0);

        //uruchomienie nasłuchowania
        f1.start();
        f2.start();

        System.out.println("Start");

        //zmienna używana do badania interwału 15s
        long pietnascie = System.currentTimeMillis() / 1000;

        while (warunek) {

            //sprawdzanie różnicy między aktualnym czasem a ostatnim czasem interwału
            if ((System.currentTimeMillis() / 1000 - pietnascie) > 14) {
                ileczasu();
                pietnascie = System.currentTimeMillis() / 1000;
            }

            //sprawdzenie, czy czas rozgrywki minął
            if ((poczatkowy + czasrozgrywki) - System.currentTimeMillis() / 1000 <= 0) {
                ileczasu();
            }

            //sprawdzanie czy obaj klienci są podłączeni
            //w przypadku zerwania połączenia obydwóch klientów serwer kończy pracę
            if (!f1.isAlive() && !f2.isAlive()) {
                warunek = false;
            }
        }

        //przerwanie działania wątków klientów
        f1.interrupt();
        f2.interrupt();

    }

}