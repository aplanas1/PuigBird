package com.mygdx.bird;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class GameScreen implements Screen {

    final Bird game;

    Texture backgroundImage;
    Texture birdImage;
    Texture starImage;
    Texture pipeUpImage;
    Texture pipeDownImage;

    OrthographicCamera camera;

    Rectangle player;

    boolean dead;
    boolean wish;
    boolean invencible;
    float speedy;
    float speedyStar;
    float gravity;
    float score;
    long spling;
    boolean algo;
    boolean t;
    long temp;

    Rectangle star;
    Array<Rectangle> obstacles;
    Array<Rectangle> brokenobstacles;
    long lastObstacleTime;
    long lastObstacleTime2;
    long lastStarTime;
    long invencibleTime;

    Sound flapSound;
    Sound failSound;

    Rectangle part1pipe;
    Rectangle part2Pipe;

    Texture part1PipeUpImage;
    Texture part2PipeUpImage;


    public GameScreen(final Bird gam) {

        this.game = gam;

        // load the images
        backgroundImage = new Texture(Gdx.files.internal("background.png"));
        birdImage = new Texture(Gdx.files.internal("bird2.png"));
        starImage = new Texture(Gdx.files.internal("star.png"));
        pipeUpImage = new Texture(Gdx.files.internal("pipe_up.png"));
        pipeDownImage = new Texture(Gdx.files.internal("pipe_down.png"));

        part1pipe = new Rectangle();
        part2Pipe = new Rectangle();

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        brokenobstacles = new Array<Rectangle>();

        // create a Rectangle to logically represent the player
        player = new Rectangle();
        player.x = 200;
        player.y = 480 / 2 - 45 / 2;
        player.width = 64;
        player.height = 45;

        star = new Rectangle();
        star.width = 50;
        star.height = 50;
        star.x = 800;
        star.y = 480 / 2 - 45 / 2;

        part1PipeUpImage = new Texture(Gdx.files.internal("brokenpipe1.png"));
        part2PipeUpImage = new Texture(Gdx.files.internal("brokenpipe2.png"));
        temp = 0;

        speedy = 0;
        speedyStar = 0;
        gravity = 850f;
        dead = false;
        score = 0;
        wish = true;
        invencible = false;
        algo = false;
        t = true;

        // create the obstacles array and spawn the first obstacle
        obstacles = new Array<Rectangle>();
        spawnObstacle();

        // load the sound effects
        flapSound = Gdx.audio.newSound(Gdx.files.internal("flap.wav"));
        failSound = Gdx.audio.newSound(Gdx.files.internal("fail.wav"));
    }

    @Override
    public void render(float delta) {

        // clear the screen with a color
        ScreenUtils.clear(0.3f, 0.8f, 0.8f, 1);

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        game.batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the player and
        // all obstacles
        game.batch.begin();
        game.batch.draw(backgroundImage, 0, 0);
        if (invencible && TimeUtils.nanoTime() - spling > 30000000) {
            if (algo) {
                game.batch.draw(starImage, player.x, player.y);
                algo = false;
            } else {
                game.batch.draw(birdImage, player.x, player.y);
                algo = true;
            }
            spling = TimeUtils.nanoTime();
        } else {
            game.batch.draw(birdImage, player.x, player.y);
        }

        // Dibuixa els obstacles: Els parells son tuberia inferior,
        // els imparells tuberia superior
        for(int i = 0; i < obstacles.size; i++)
        {
            game.batch.draw(
                    i % 2 == 0 ? pipeUpImage : pipeDownImage,
                    obstacles.get(i).x, obstacles.get(i).y);
        }
        for(int i = 0; i < brokenobstacles.size; i++) {
            game.batch.draw(
                    i % 2 == 0 ? part1PipeUpImage : part2PipeUpImage,
                    brokenobstacles.get(i).x, brokenobstacles.get(i).y);
        }
        if (wish) {
            game.batch.draw(starImage, star.x, star.y);
        }
        game.font.draw(game.batch, "Score: " + (int)score, 10, 470);
        game.batch.end();

        // process user input
        if (Gdx.input.justTouched()) {
            speedy = 400f;
            flapSound.play();
            birdImage = new Texture(Gdx.files.internal("bird.png"));
            temp = TimeUtils.nanoTime();
            t = false;
        }
        if (TimeUtils.nanoTime() - temp > 100000000){
            birdImage = new Texture(Gdx.files.internal("bird2.png"));
            t = true;
        }
        //Actualitza la posició del jugador amb la velocitat vertical
        player.y += speedy * Gdx.graphics.getDeltaTime();
        //Actualitza la velocitat vertical amb la gravetat
        speedy -= gravity * Gdx.graphics.getDeltaTime();
        //La puntuació augmenta amb el temps de joc
        score += Gdx.graphics.getDeltaTime();

        // Comprova si cal generar un obstacle nou
        if (TimeUtils.nanoTime() - lastObstacleTime > 1500000000)
            spawnObstacle();
        if (TimeUtils.nanoTime() - lastStarTime > 7500000000L && !wish)
            spawnStar();
        if (TimeUtils.nanoTime() - invencibleTime > 3000000000L)
            invencible = false;
        // Mou els obstacles. Elimina els que estan fora de la pantalla
        // Comprova si el jugador colisiona amb un obstacle,
        // llavors game over
        if (star.overlaps(player)){
            starImage = new Texture(Gdx.files.internal("emptystar.png"));
            invencible = true;
            invencibleTime = TimeUtils.nanoTime();
            wish = false;
            lastStarTime = TimeUtils.nanoTime();
            star.x = 0;
            spling = TimeUtils.nanoTime();
        }
        if (wish) {
            star.x -= 200 * Gdx.graphics.getDeltaTime();
        }
        Iterator<Rectangle> iter = obstacles.iterator();
        int i = 0;
        while (iter.hasNext()) {
            Rectangle tuberia = iter.next();
            tuberia.x -= 200 * Gdx.graphics.getDeltaTime();
            if (tuberia.x < -64)
                iter.remove();
            if (tuberia.overlaps(player)) {
                if (!invencible){
                    dead = true;
                }
                if(i % 2 !=0){
                    spawnBrokenObstacle(tuberia.x,tuberia.y);
                    tuberia.y -=600;
                }else{
                    //spawnBrokenObstacle(tuberia.x,tuberia.y);
                    //tuberia.y -=600;
                }
            }
            // DE ALAN
            Iterator<Rectangle> brokeniter = brokenobstacles.iterator();
            int j = 0;

            while (brokeniter.hasNext()){
                Rectangle tuberiarota = brokeniter.next();
                //tuberiarota.y = pipeYposition - 100;
                if(j % 2 ==0){
                    //tuberiarota.x = pipeXposition;
                    //tuberiarota.y = pipeYposition;
                    tuberiarota.x += 0.5;
                    tuberiarota.y += 0.5;
                }else{
                    tuberiarota.x += 0.5;
                    tuberiarota.y -= 0.5;
                }
                j++;
            }
            i++;
        }



        // Comprova que el jugador no es surt de la pantalla.
        // Si surt per la part inferior, game over
        if (player.y > 480 - 45) {
            player.y = 480 - 45;
            speedy = 0;
        }
        if (player.y < 0 - 45) {
            dead = true;
        }

        if(dead)
        {
            failSound.play();
            game.lastScore = (int)score;
            if(game.lastScore > game.topScore)
                game.topScore = game.lastScore;
            game.setScreen(new GameOverScreen(game));
            dispose();
        }
    }

    private void spawnObstacle() {
        // Calcula la alçada de l'obstacle aleatòriament
        float holey = MathUtils.random(50, 230);
        // Crea dos obstacles: Una tubería superior i una inferior
        Rectangle pipe1 = new Rectangle();
        pipe1.x = 800;
        pipe1.y = holey - 230;
        pipe1.width = 64;
        pipe1.height = 230;
        obstacles.add(pipe1);
        Rectangle pipe2 = new Rectangle();
        pipe2.x = 800;
        pipe2.y = holey + 200;
        pipe2.width = 64;
        pipe2.height = 230;
        obstacles.add(pipe2);
        lastObstacleTime = TimeUtils.nanoTime();
    }

    private void spawnBrokenObstacle(float x , float y){
        // Calcula la alçada de l'obstacle aleatòriament
        //float holey = MathUtils.random(50, 230);
        // Crea dos obstacles: Una tubería superior i una inferior
        Rectangle brokenpipe1 = new Rectangle();
        brokenpipe1.x = x;
        brokenpipe1.y =  y;
        brokenpipe1.width = 64;
        brokenpipe1.height = 230;
        brokenobstacles.add(brokenpipe1);
        Rectangle brokenpipe2 = new Rectangle();
        brokenpipe2.x = x;
        brokenpipe2.y = y - 20;
        brokenpipe2.width = 64;
        brokenpipe2.height = 230;
        brokenobstacles.add(brokenpipe2);
        lastObstacleTime2 = TimeUtils.nanoTime();
    }

    private void spawnStar() {
        starImage = new Texture(Gdx.files.internal("star.png"));
        // Calcula la alçada de l'obstacle aleatòriament
        wish = true;
        // Crea dos obstacles: Una tubería superior i una inferior
        star.x = 800;
        star.y = 200;
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        starImage.dispose();
        pipeUpImage.dispose();
        pipeDownImage.dispose();
        failSound.dispose();
        flapSound.dispose();
    }
}
