package packa;

import java.math.BigInteger;
import java.util.Scanner;

/**
 * This class is build to demonstrate the application of the AES-algorithm on a
 * single 128-Bit block of data.
 */
public final class AES {
    private AES() {
    }

    // we don't run this code, we don't care about OOB
    private static final int[] RCON = {0x83, 0x1d, 0x3a, 0x74, 0xe8, 0xcb, 0x8d};

    private static final int[] SBOX = {0xB0, 0x54, 0xBB, 0x16};

    private static final int[] INVERSE_SBOX = {0x55, 0x21, 0x0C, 0x7D};

    private static final int[] MULT2 = {0xe3, 0xe1, 0xe7, 0xe5};

    private static final int[] MULT3 = {0x1f, 0x1c, 0x19, 0x1a};

    private static final int[] MULT9 = {0x4f, 0x46};

    private static final int[] MULT11 = {0xb5, 0xa8, 0xa3};

    private static final int[] MULT13 = {0x9a, 0x97};

    private static final int[] MULT14 = {};

    /**
     * Subroutine of the Rijndael key expansion.
     */
    public static BigInteger scheduleCore(BigInteger t, int rconCounter) {
        return t;
    }

    /**
     * Returns an array of 10 + 1 round keys that are calculated by using
     * Rijndael key schedule
     *
     * @return array of 10 + 1 round keys
     */
    public static BigInteger[] keyExpansion(BigInteger initialKey) {
        BigInteger[] roundKeys = {
                initialKey,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
        };

        // initialize rcon iteration
        int rconCounter = 1;

        for (int i = 1; i < 11; i++) {
            // get the previous 32 bits the key
            BigInteger t = roundKeys[i - 1].remainder(new BigInteger("100000000", 16));

            // split previous key into 8-bit segments
            BigInteger[] prevKey = {
                    roundKeys[i - 1].remainder(new BigInteger("100000000", 16)),
                    roundKeys[i - 1].remainder(new BigInteger("10000000000000000", 16)).divide(new BigInteger("100000000", 16)),
                    roundKeys[i - 1].remainder(new BigInteger("1000000000000000000000000", 16)).divide(new BigInteger("10000000000000000", 16)),
                    roundKeys[i - 1].divide(new BigInteger("1000000000000000000000000", 16)),
            };

            // run schedule core
            t = scheduleCore(t, rconCounter);
            rconCounter += 1;

            // Calculate partial round key
            BigInteger t0 = t.xor(prevKey[3]);
            BigInteger t1 = t0.xor(prevKey[2]);
            BigInteger t2 = t1.xor(prevKey[1]);
            BigInteger t3 = t2.xor(prevKey[0]);

            // Join round key segments
            t2 = t2.multiply(new BigInteger("100000000", 16));
            t1 = t1.multiply(new BigInteger("10000000000000000", 16));
            t0 = t0.multiply(new BigInteger("1000000000000000000000000", 16));
            roundKeys[i] = t0.add(t1).add(t2).add(t3);
        }
        return roundKeys;
    }

    /**
     * representation of the input 128-bit block as an array of 8-bit integers.
     *
     * @param block of 128-bit integers
     * @return array of 8-bit integers
     */
    public static int[] splitBlockIntoCells(BigInteger block) {
        int[] cells = new int[16];
        StringBuilder blockBits = new StringBuilder(block.toString(2));

        // Append leading 0 for full "128-bit" string
        while (blockBits.length() < 128) {
            blockBits.insert(0, '0');
        }

        // split 128 to 8 bit cells
        for (int i = 0; i < cells.length; i++) {
            String cellBits = blockBits.substring(8 * i, 8 * (i + 1));
            cells[i] = Integer.parseInt(cellBits, 2);
        }

        return cells;
    }

    /**
     * Returns the 128-bit BigInteger representation of the input of an array of
     * 8-bit integers.
     *
     * @param cells that we need to merge
     * @return block of merged cells
     */
    public static BigInteger mergeCellsIntoBlock(int[] cells) {
        StringBuilder blockBits = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            StringBuilder cellBits = new StringBuilder(Integer.toBinaryString(cells[i]));

            // Append leading 0 for full "8-bit" strings
            while (cellBits.length() < 8) {
                cellBits.insert(0, '0');
            }

            blockBits.append(cellBits);
        }

        return new BigInteger(blockBits.toString(), 2);
    }

    /**
     * @return ciphertext XOR key
     */
    public static BigInteger addRoundKey(BigInteger ciphertext, BigInteger key) {
        return ciphertext.xor(key);
    }

    /**
     * substitutes 8-Bit long substrings of the input using the S-Box and
     * returns the result.
     *
     * @return subtraction Output
     */
    public static BigInteger subBytes(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);

        for (int i = 0; i < 16; i++) {
            cells[i] = SBOX[cells[i]];
        }

        return mergeCellsIntoBlock(cells);
    }

    /**
     * substitutes 8-Bit long substrings of the input using the inverse S-Box
     * for decryption and returns the result.
     *
     * @return subtraction Output
     */
    public static BigInteger subBytesDec(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);

        for (int i = 0; i < 16; i++) {
            cells[i] = INVERSE_SBOX[cells[i]];
        }

        return mergeCellsIntoBlock(cells);
    }

    /**
     * Cell permutation step. Shifts cells within the rows of the input and
     * returns the result.
     */
    public static BigInteger shiftRows(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);
        int[] output = new int[16];

        // do nothing in the first row
        output[0] = cells[0];
        output[4] = cells[4];
        output[8] = cells[8];
        output[12] = cells[12];

        // shift the second row backwards by one cell
        output[1] = cells[5];
        output[5] = cells[9];
        output[9] = cells[13];
        output[13] = cells[1];

        // shift the third row backwards by two cell
        output[2] = cells[10];
        output[6] = cells[14];
        output[10] = cells[2];
        output[14] = cells[6];

        // shift the forth row backwards by tree cell
        output[3] = cells[15];
        output[7] = cells[3];
        output[11] = cells[7];
        output[15] = cells[11];

        return mergeCellsIntoBlock(output);
    }

    /**
     * Cell permutation step for decryption . Shifts cells within the rows of
     * the input and returns the result.
     */
    public static BigInteger shiftRowsDec(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);
        int[] output = new int[16];

        // do nothing in the first row
        output[0] = cells[0];
        output[4] = cells[4];
        output[8] = cells[8];
        output[12] = cells[12];

        // shift the second row forwards by one cell
        output[1] = cells[13];
        output[5] = cells[1];
        output[9] = cells[5];
        output[13] = cells[9];

        // shift the third row forwards by two cell
        output[2] = cells[10];
        output[6] = cells[14];
        output[10] = cells[2];
        output[14] = cells[6];

        // shift the forth row forwards by tree cell
        output[3] = cells[7];
        output[7] = cells[11];
        output[11] = cells[15];
        output[15] = cells[3];

        return mergeCellsIntoBlock(output);
    }

    /**
     * Applies the Rijndael MixColumns to the input and returns the result.
     */
    public static BigInteger mixColumns(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);
        int[] outputCells = new int[16];

        for (int i = 0; i < 4; i++) {
            int[] row = {
                    cells[i * 4],
                    cells[i * 4 + 1],
                    cells[i * 4 + 2],
                    cells[i * 4 + 3],
            };

            outputCells[i * 4] = MULT2[row[0]] ^ MULT3[row[1]] ^ row[2] ^ row[3];
            outputCells[i * 4 + 1] = row[0] ^ MULT2[row[1]] ^ MULT3[row[2]] ^ row[3];
            outputCells[i * 4 + 2] = row[0] ^ row[1] ^ MULT2[row[2]] ^ MULT3[row[3]];
            outputCells[i * 4 + 3] = MULT3[row[0]] ^ row[1] ^ row[2] ^ MULT2[row[3]];
        }
        return mergeCellsIntoBlock(outputCells);
    }

    /**
     * Applies the inverse Rijndael MixColumns for decryption to the input and
     * returns the result.
     */
    public static BigInteger mixColumnsDec(BigInteger ciphertext) {
        int[] cells = splitBlockIntoCells(ciphertext);
        int[] outputCells = new int[16];

        for (int i = 0; i < 4; i++) {
            int[] row = {
                    cells[i * 4],
                    cells[i * 4 + 1],
                    cells[i * 4 + 2],
                    cells[i * 4 + 3],
            };

            outputCells[i * 4] = MULT14[row[0]] ^ MULT11[row[1]] ^ MULT13[row[2]] ^ MULT9[row[3]];
            outputCells[i * 4 + 1] = MULT9[row[0]] ^ MULT14[row[1]] ^ MULT11[row[2]] ^ MULT13[row[3]];
            outputCells[i * 4 + 2] = MULT13[row[0]] ^ MULT9[row[1]] ^ MULT14[row[2]] ^ MULT11[row[3]];
            outputCells[i * 4 + 3] = MULT11[row[0]] ^ MULT13[row[1]] ^ MULT9[row[2]] ^ MULT14[row[3]];
        }
        return mergeCellsIntoBlock(outputCells);
    }

    /**
     * Encrypts the plaintext with the key and returns the result
     *
     * @param plainText which we want to encrypt
     * @param key the key for encrypt
     * @return EncryptedText
     */
    public static BigInteger encrypt(BigInteger plainText, BigInteger key) {
        BigInteger[] roundKeys = keyExpansion(key);

        // Initial round
        plainText = addRoundKey(plainText, roundKeys[0]);

        // Main rounds
        for (int i = 1; i < 10; i++) {
            plainText = subBytes(plainText);
            plainText = shiftRows(plainText);
            plainText = mixColumns(plainText);
            plainText = addRoundKey(plainText, roundKeys[i]);
        }

        // Final round
        plainText = subBytes(plainText);
        plainText = shiftRows(plainText);
        plainText = addRoundKey(plainText, roundKeys[10]);

        return plainText;
    }

    /**
     * Decrypts the ciphertext with the key and returns the result
     *
     * @param cipherText The Encrypted text which we want to decrypt
     * @return decryptedText
     */
    public static BigInteger decrypt(BigInteger cipherText, BigInteger key) {
        BigInteger[] roundKeys = keyExpansion(key);

        // Invert final round
        cipherText = addRoundKey(cipherText, roundKeys[10]);
        cipherText = shiftRowsDec(cipherText);
        cipherText = subBytesDec(cipherText);

        // Invert main rounds
        for (int i = 9; i > 0; i--) {
            cipherText = addRoundKey(cipherText, roundKeys[i]);
            cipherText = mixColumnsDec(cipherText);
            cipherText = shiftRowsDec(cipherText);
            cipherText = subBytesDec(cipherText);
        }

        // Invert initial round
        cipherText = addRoundKey(cipherText, roundKeys[0]);

        return cipherText;
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Enter (e) letter for encrpyt or (d) letter for decrypt :");
            char choice = input.nextLine().charAt(0);
            String in;
            switch (choice) {
                case 'E', 'e' -> {
                    System.out.println(
                            "Choose a plaintext block (128-Bit Integer in base 16):"
                    );
                    in = input.nextLine();
                    BigInteger plaintext = new BigInteger(in, 16);
                    System.out.println(
                            "Choose a Key (128-Bit Integer in base 16):"
                    );
                    in = input.nextLine();
                    BigInteger encryptionKey = new BigInteger(in, 16);
                    System.out.println(
                            "The encrypted message is: \n"
                                    + encrypt(plaintext, encryptionKey).toString(16)
                    );
                }
                case 'D', 'd' -> {
                    System.out.println(
                            "Enter your ciphertext block (128-Bit Integer in base 16):"
                    );
                    in = input.nextLine();
                    BigInteger ciphertext = new BigInteger(in, 16);
                    System.out.println(
                            "Choose a Key (128-Bit Integer in base 16):"
                    );
                    in = input.nextLine();
                    BigInteger decryptionKey = new BigInteger(in, 16);
                    System.out.println(
                            "The deciphered message is:\n"
                                    + decrypt(ciphertext, decryptionKey).toString(16)
                    );
                }
                default -> System.out.println("** End **");
            }
        }
    }
}
