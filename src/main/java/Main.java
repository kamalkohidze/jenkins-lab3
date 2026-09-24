import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Main {

    static final String FLAG = "01111110";

    // Убирает лишний ноль, который стоит после пяти единиц подряд
    public static String destuffBits(String bits) {
        StringBuilder sb = new StringBuilder();
        int ones = 0;
        for (char c : bits.toCharArray()) {
            if (c == '1') {
                ones++;
                sb.append(c);
            } else {
                if (ones != 5) sb.append(c); // если единиц было ровно 5, этот ноль лишний, пропускаем
                ones = 0;
            }
        }
        return sb.toString();
    }

    // Проверка: последний байт должен равняться сумме всех остальных (по модулю 256)
    public static boolean checksumOk(byte[] bytes) {
        if (bytes.length < 2) return false;
        int sum = 0;
        for (int i = 0; i < bytes.length - 1; i++) sum += bytes[i] & 0xFF;
        return (sum % 256) == (bytes[bytes.length - 1] & 0xFF);
    }

    // Актор-приёмник: получает поток битов, чистит и проверяет
    public static class Parser extends AbstractActor {
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

        @Override
        public Receive createReceive() {
            return receiveBuilder().match(String.class, raw -> {
                log.info("Получен поток: {}", raw);

                if (raw.length() < 16 || !raw.startsWith(FLAG) || !raw.endsWith(FLAG)) {
                    log.warning("Кадр отброшен: нет меток начала и конца");
                    return;
                }

                String clean = destuffBits(raw.substring(8, raw.length() - 8));
                log.info("После удаления лишних нулей: {}", clean);

                if (clean.length() % 8 != 0) {
                    log.warning("Кадр отброшен: длина данных не кратна 8");
                    return;
                }

                byte[] bytes = new byte[clean.length() / 8];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) Integer.parseInt(clean.substring(i * 8, i * 8 + 8), 2);
                }

                if (checksumOk(bytes)) {
                    log.info("Кадр принят, контрольная сумма верна");
                } else {
                    log.warning("Кадр отброшен: контрольная сумма не совпала");
                }
            }).build();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Строки из задания: показываем, как исчезают лишние нули
        String[] demos = {
                "0111111011011111011111011001001111110",
                "01111110000100100100100100100001101111110"
        };
        for (String d : demos) {
            String inner = d.substring(8, d.length() - 8);
            System.out.println("Из задания: " + inner + "  ->  " + destuffBits(inner));
        }

        ActorSystem system = ActorSystem.create("iot");
        ActorRef parser = system.actorOf(Props.create(Parser.class), "parser");

        // Правильный кадр: данные FF FF, вторая FF это контрольная сумма
        String good = FLAG + "111110" + "111110" + "111110" + "1" + FLAG;
        // Испорченный: в данных FE FF, а контрольная сумма осталась от FF
        String bad = FLAG + "111110" + "110" + "111110" + "111" + FLAG;

        parser.tell(good, ActorRef.noSender());
        parser.tell(bad, ActorRef.noSender());

        Thread.sleep(1000);
        system.terminate();
    }
}