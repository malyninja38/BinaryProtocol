# BinaryProtocol


# Overview

Prosta gra polegająca na odgadnięciu wylosowanej przez serwer liczby. 
Klienci (maksymalnie dwóch) łączą się z serwerem za pomocą autorskiego protokołu binarnego, który został stworzony na potrzeby tego programu.

# Description

Implementacja protokołu komunikacyjnego, aplikacji klienckiej oraz aplikacji serwerowej w języku Java.
Komunikacja pomiędzy dwoma klientami odbywa się poprzez serwer, w oparciu o połączenowy protokół komunikacyjny.
Wszystkie dane przesyłane są w postaci binarnej.

Klient nawiązuje połączenie z zerwerem i uzyskuje identyfikator sesji wygenerowany przez serwer. 
Ten z kolei wyznacza maksymalny czas trwania rozgrywki, a następnie losuje liczbę, którą klienci muszą odgadnąc.
Następnie co 15 sekund wysyła komunikaty o tym, ile czasu jeszcze pozostało. Informuje również klientów, czy wpisana przez nich liczba jest za duża lub za mała.



# Tools

### Software:
- IntelliJ IDEA

### Languages
- Java


# License

MIT


# Credits

### Creators:
- Paulina Kukuła
- Dominik Hażak

