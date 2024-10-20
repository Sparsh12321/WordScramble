package com.example.wordscramble

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*

interface WordApi {
    @GET("/word?number=1")
    fun getRandomWord(): Call<List<String>>
}

class MainActivity : AppCompatActivity() {
    private lateinit var targetWord: String
    private var attempts: Int = 0
    private lateinit var feedback: TextView
    private lateinit var attemptsView: TextView
    private lateinit var resultView: TextView
    private lateinit var userGuess: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feedback = findViewById(R.id.feedback)
        attemptsView = findViewById(R.id.attempts)
        resultView = findViewById(R.id.result)
        userGuess = findViewById(R.id.userGuess)
        val submitGuess: Button = findViewById(R.id.submitGuess)

        // Set a constraint for the user input to 4 letters only
        userGuess.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))

        // Load previous state if exists
        loadGameState()

        // Fetch random word from API
        fetchFourLetterWord()

        submitGuess.setOnClickListener {
            checkGuess()
        }
    }

    private fun loadGameState() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("gameState", MODE_PRIVATE)
        attempts = sharedPreferences.getInt("attempts", 0)
        attemptsView.text = "Attempts: $attempts"
    }

    private fun fetchFourLetterWord() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://random-word-api.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val wordApi = retrofit.create(WordApi::class.java)

        // Keep fetching until a 4-letter word is found
        wordApi.getRandomWord().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val fetchedWord = response.body()!![0]
                    if (fetchedWord.length == 4) {
                        targetWord = fetchedWord // Accept the 4-letter word
                    } else {
                        // If the word is not 4 letters, fetch again
                        fetchFourLetterWord()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load word", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error fetching word", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkGuess() {
        // Check if targetWord is initialized
        if (!::targetWord.isInitialized) {
            Toast.makeText(this, "Word is not yet loaded. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = userGuess.text.toString().lowercase(Locale.getDefault())

        // Check if the guess is exactly 4 letters
        if (guess.length != 4) {
            Toast.makeText(this, "Please enter exactly 4 letters", Toast.LENGTH_SHORT).show()
            return
        }

        attempts++
        attemptsView.text = "Attempts: $attempts"

        if (guess == targetWord) {
            resultView.text = "You guessed it!"
            saveGameState(true) // Reset attempts
            fetchFourLetterWord() // Fetch a new word for a new game
            return // Exit the method to avoid further processing
        }

        // Continue with feedback logic if the guess is incorrect
        val feedbackBuilder = StringBuilder("Feedback: ")
        for (i in guess.indices) {
            when {
                i < targetWord.length && guess[i] == targetWord[i] -> {
                    feedbackBuilder.append(guess[i]).append(" ")
                }
                targetWord.contains(guess[i]) -> {
                    feedbackBuilder.append("? ")
                }
                else -> {
                    feedbackBuilder.append("X ")
                }
            }
        }
        feedback.text = feedbackBuilder.toString()

        // Check if attempts reached maximum limit
        if (attempts >= 5) {
            resultView.text = "Game over! The word was: $targetWord"
            saveGameState(true) // Reset attempts
            fetchFourLetterWord() // Fetch a new word for a new game
        }
    }

    private fun saveGameState(isNewGame: Boolean) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("gameState", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if (isNewGame) {
            editor.putInt("attempts", 0) // Reset attempts
        } else {
            editor.putInt("attempts", attempts)
        }
        editor.apply()
    }

    private fun resetGame() {
        attempts = 0
        attemptsView.text = "Attempts: 0"
        userGuess.text.clear()
        feedback.text = ""
        resultView.text = ""
        saveGameState(true) // Reset attempts in saved state
        fetchFourLetterWord() // Fetch a new word for the new game
    }
}
