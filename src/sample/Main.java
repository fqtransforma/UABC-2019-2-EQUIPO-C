package sample;

import botones.Button;
import controller.ControlInput;
import controller.ControlsSetup;

import display.Background;
import entidades.Entity;
import entidades.MovingIsoEntity;
import gameObjeto.*;
import gameObjeto.basura.Basura;
import gameObjeto.basura.basuraOrganica.BasuraBanana;
import gameObjeto.basura.basuraOrganica.BasuraManzana;
import gameObjeto.basura.basuraOrganica.BasuraSandia;
import gameObjeto.basura.basuraPapel.BasuraBolaPapel;
import gameObjeto.basura.basuraPapel.BasuraPapelAvion;
import gameObjeto.basura.basuraPapel.BasuraPeriodico;
import gameObjeto.basura.basuraPlastico.BasuraBotella;
import gameObjeto.basura.basuraPlastico.BasuraPlastico;
import gameObjeto.basura.basuraVidrio.BasuraBotellaRoto;
import gameObjeto.basura.basuraVidrio.BasuraFoco;
import gameObjeto.basura.basuraVidrio.BasuraVentanaRoto;
import gameObjeto.boteBasura.BoteBasura;
import gameObjeto.boteBasura.BotePlastico;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import people.Player;
import enums.Direccion;
import reproductor.MusicPlayer;
import resourceLoaders.AudioLoader;
import resourceLoaders.ImageLoader;
import teclado.TecladoFX;

import javax.swing.*;
import java.util.*;


public class Main extends Application {

    public static final int WIDTH = 1024;       //Constantes para cambiar el size del canvas de manera remota.
    public static final int HEIGHT = 600;       //Podrian ser variables si queremos hacer que se escale el juego y sus elementos.
    //Pd. la resolucion COMPLETA de la tablet es 1280 x 800

    private Canvas canvas;
    private Group grupo;
    private Scene escena;
    private GraphicsContext gc;

    private MusicPlayer reproductor;

    private static StateGame stateGame;

    private Button botonA;
    private Button botonB;
    private Button dpad;
    private TecladoFX teclado;
    public static Button botonJugar=new Button(300,200,50,60, ImageLoader.spriteBotonJugar);
    public static Button botonInstruccion=new Button(300,300,50,60, ImageLoader.spriteBotonInstruccion);
    public static Button botonSalir=new Button(300,400,50,60, ImageLoader.spriteBotonSalir);

    final long startNanoTime = System.nanoTime();

    public static final double DASH_SPEED_MULT = 4;
    public static final double DIAG_SPEED_MULT = Math.sqrt(2);
    private static int dashCooldown;        //En frames 60 frames - 1 seg
    private final double maxDashFrames = 10; //En Frames
    private double dashFrames;

    private boolean intro = false;
    private boolean shift = false;  //False = derecha

    private int puntaje;        //puntaje, como es un atributo de clase, se inicializa en 0 por default.
    Text texto = new Text();

    /*
    *  ArrayBasura podria ser una clase estatica en la que se inicializara lo que ocupamos en un static block, llamamos a un metodo
    *  que devuelva un array de entidades que ya tenga todas las basuras. Asi te libras del foreach en initializeArrayEntidad. Ademas
    *  te libras de cosas redundantes como Main.getArrayBasura().getArrayBasura(); [TouchControl : 32]
    */
    private static ArrayBasura arrayBasura = new ArrayBasura();
    private static Player jugador = new Player( 100, 100, 32, 48, 0.5);
    private static Camion camion = new Camion(100, 200, 200, 100, 0.5);
    private static BotePlastico boteAzul = new BotePlastico( 50, 250, 50, 50, 1);
    Iterator<Basura> array = arrayBasura.getArrayBasura().iterator();
    List<Basura> remove = new ArrayList();
    private ArrayList<Entity> arrayEntidad;
    private Background bg;

    private Comparator cmpArrayEntidad;

    public int i;
    TextInputDialog dialog = new TextInputDialog("walter");

    @Override
    public void init() throws Exception {
        stateGame = StateGame.menu;

        initializeGroup();
        initializeControls();
        initializeCanvas();
        initializeReproductor();
        initializeArrayEntidad();
        initializeUtilities();
        inicializaBotonMenu();

        arrayEntidad.sort(cmpArrayEntidad);    //Le hacemos un sort antes de empezar para que no tarde la primera vez que lo haga
                                                            //dentro del juego

        bg = new Background();
        texto.setX(0);
        texto.setY(40);
        texto.setFont(Font.font("Verdana", 30));

        addComponet();

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setupPrimaryStage(primaryStage);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - startNanoTime) / 1000000000.0;

                if(intro) {
                    showLevel(t);
                }
                else {
                    updateLogic();
                    updateGraphic(gc, t);
                }
            }
        }.start();

        reproductor.fadeInPlay(0.6);

        primaryStage.show();
    }



    ////////////////////////////////////////////
    /*             Bloque de setup            */
    ////////////////////////////////////////////

    //        Define propiedades basicas del stage.
    private void setupPrimaryStage(Stage primaryStage) {
        primaryStage.centerOnScreen();
        primaryStage.requestFocus();
        primaryStage.setTitle("Proyecto - PP");
        primaryStage.setScene(escena);    //Uso una variable Scene para poder utilizar los listeners del teclado.
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

    }

    private void initializeControls() {
        botonA = ControlsSetup.getBotonA();
        botonB = ControlsSetup.getBotonB();
        dpad = ControlsSetup.getDpad();

        teclado = ControlsSetup.getTeclado();
        teclado.setKeyboardOnScene(escena);
    }

    private void initializeGroup() {
        //Aqui va la configuracion inicial del grupo y escena
        grupo = new Group();
        escena = new Scene(grupo, WIDTH, HEIGHT);
    }

    private void initializeCanvas() {
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();     //Muevo esto aqui por relacion al canvas.
    }

    private void initializeReproductor() {
        reproductor = new MusicPlayer(AudioLoader.muteCity);
        reproductor.setVolume(0);
    }

    private void initializeArrayEntidad() {
        arrayEntidad = new ArrayList<>();

        arrayEntidad.addAll(arrayBasura.getArrayBasura());
        arrayEntidad.add(jugador);
        arrayEntidad.add(camion);
        arrayEntidad.add(boteAzul);
    }

    private void initializeUtilities() {

        //Comparador para el sort de arrayEntidad con Collection.sort
        cmpArrayEntidad = new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return Double.compare(o1.getY() + o1.getHeight(), o2.getY() + o2.getHeight());
            }
        };
    }

    public void addComponet()
    {
        grupo.getChildren().clear();
        grupo.getChildren().add(canvas);

        if(stateGame==StateGame.menu)
        {
            grupo.getChildren().addAll(botonInstruccion,botonJugar,botonSalir);
        }

        if(stateGame==StateGame.playing)
        {
            grupo.getChildren().addAll(botonA, botonB, dpad, texto);
        }

        //else if? switch tal vez para cuando tengamos mas?
        if(stateGame==StateGame.gameOver)
        {

        }
    }

    ////////////////////////////////////////////
    /*          Bloque de setup [FIN]         */
    ////////////////////////////////////////////

    private void showLevel(double t) {

        if (!shift) {
            bg.setBackgroundX(bg.getBackgroundX() - 11);
            if(bg.getBackgroundX()-(WIDTH+100) < -bg.getGameBg(1).getWidth())
                shift = true;
        } else {
            bg.setBackgroundX(bg.getBackgroundX() + 11);
            if (bg.getBackgroundX() >= 0) {
                bg.setBackgroundX(0);
                shift = false;
                intro = false;
            }
        }
        //System.out.printf("%d\r", bg.getBackgroundX());
        updateGraphic(gc, t);
    }

    public void updateLogic()
    {

        if(stateGame==StateGame.playing)
        {

            Collections.sort(arrayEntidad, cmpArrayEntidad);    //Instancie el comparador en el initializeUtilities para que no se cree uno nuevo cada vez.

            texto.setText("EL puntaje es:"+puntaje+"                                   Gasolina:"+String.format("%.1f",camion.getGasolina()/10));

            updatePlayerMovement();
            collisionDetection();
            updateEntitiesInScreen();


            jugador.move();

        }

        updateGameState();

    }

    private void updatePlayerMovement() {

        //Tanto el touch como el teclado comparten funcionamiento, y se realizan cambios segun lo que se tenga presionado
        //en este metodo.

        if(!jugador.isDashing()) {

            if (ControlInput.isButtonPressed("UP") && !ControlInput.isButtonPressed("DOWN")) {
                jugador.setDx(0);
                jugador.setDy(-Player.SPEED);

                jugador.setDireccion(Direccion.arriba);
            }

            if (ControlInput.isButtonPressed("DOWN") && !ControlInput.isButtonPressed("UP")) {
                jugador.setDx(0);
                jugador.setDy(Player.SPEED);

                jugador.setDireccion(Direccion.abajo);
            }

            if (ControlInput.isButtonPressed("RIGHT") && !ControlInput.isButtonPressed("LEFT")) {
                jugador.setDx(Player.SPEED);
                jugador.setDy(0);

                if (ControlInput.isButtonPressed("UP")) {
                    jugador.setDx(Player.SPEED / DIAG_SPEED_MULT);
                    jugador.setDy(-Player.SPEED / DIAG_SPEED_MULT);
                } else if (ControlInput.isButtonPressed("DOWN")) {
                    jugador.setDx(Player.SPEED / DIAG_SPEED_MULT);
                    jugador.setDy(Player.SPEED / DIAG_SPEED_MULT);
                }

                jugador.setDireccion(Direccion.derecha);
            }

            if (ControlInput.isButtonPressed("LEFT") && !ControlInput.isButtonPressed("RIGHT")) {
                jugador.setDx(-Player.SPEED);
                jugador.setDy(0);

                if (ControlInput.isButtonPressed("UP")) {
                    jugador.setDx(-Player.SPEED / DIAG_SPEED_MULT);
                    jugador.setDy(-Player.SPEED / DIAG_SPEED_MULT);
                } else if (ControlInput.isButtonPressed("DOWN")) {
                    jugador.setDx(-Player.SPEED / DIAG_SPEED_MULT);
                    jugador.setDy(Player.SPEED / DIAG_SPEED_MULT);
                }

                jugador.setDireccion(Direccion.izquierda);
            }

            if (ControlInput.isButtonPressed("RIGHT") && ControlInput.isButtonPressed("LEFT")) {
                jugador.setDx(0);

                if (ControlInput.isButtonPressed("UP")) {
                    jugador.setDy(-Player.SPEED);

                    jugador.setDireccion(Direccion.arriba);
                } else if (ControlInput.isButtonPressed("DOWN")) {
                    jugador.setDy(Player.SPEED);

                    jugador.setDireccion(Direccion.abajo);
                } else {
                    jugador.setDy(0);
                }
            }

            if (ControlInput.isButtonPressed("UP") && ControlInput.isButtonPressed("DOWN")) {
                jugador.setDy(0);

                if (ControlInput.isButtonPressed("RIGHT")) {
                    jugador.setDx(Player.SPEED);

                    jugador.setDireccion(Direccion.derecha);
                } else if (ControlInput.isButtonPressed("LEFT")) {
                    jugador.setDx(-Player.SPEED);

                    jugador.setDireccion(Direccion.izquierda);
                } else {
                    jugador.setDx(0);
                }
            }

            if (!ControlInput.isButtonPressed("LEFT") && !ControlInput.isButtonPressed("RIGHT") &&
                    !ControlInput.isButtonPressed("UP") && !ControlInput.isButtonPressed("DOWN")) {
                jugador.setDx(-Camion.SPEED);
                jugador.setDy(0);

            }


            //Esto es para agarrar y soltar basura
            if (ControlInput.isButtonPressed("S")) {

                if (!ControlInput.isAltButtonA()) {
                    if (!jugador.isCargandoBasura()) {

                        for (Basura basura :
                                arrayBasura.getArrayBasura()) {
                            if (basura.isNextToPlayer() && !jugador.isCargandoBasura()) {      //Mas de una basura se podia mover
                                jugador.setCargandoBasura(true);                               //por que no consideramos que el jugador
                                basura.setMoving(true);                                 //podia volverse ocupado dentro de este mismo
                                jugador.setBasura(basura);                              //
                            }
                        }
                    } else {
/*
                        for (Basura basura :
                                Main.getArrayBasura().getArrayBasura()) {
                            if (basura.isMoving() && jugador.isCargandoBasura()) {
                                jugador.setCargandoBasura(false);
                                basura.setMoving(false);
                            }
                        }*/
                        jugador.getBasura().setMoving(false);
                        jugador.setBasura(null);
                        jugador.setCargandoBasura(false);
                    }

                    ControlInput.setAltButtonA(true);   //Es un switch, para saber si soltaron o no la tecla.
                }

            } else {
                ControlInput.setAltButtonA(false);
            }


            //Este es el dash
            if (ControlInput.isButtonPressed("D")) {
                if ((jugador.getDx() != 0 || jugador.getDy() != 0) && !ControlInput.isAltButtonB() && dashCooldown == 0) {     //Si se está moviendo hacia alguna direccion.
                    jugador.setDashing(true);
                    ControlInput.setAltButtonB(true);
                }
            } else {        //Necesitamos que pueda realizar su funcionalidad UNA vez hasta que lo vuelva a presionar.
                //Con esto lo que hago es obligar al usuario a levantar el dedo de la tecla, y solo despues
                //de que lo haga es que puede volver a utilizar el boton. Lo mismo con el de arriba.
                ControlInput.setAltButtonB(false);
            }

            if (dashCooldown > 0) {      //Contador para evitar que hagan dash seguidos.
                dashCooldown--;
            }

        } else {

            if(dashFrames != maxDashFrames) {
                if(dashFrames == 0) {
                    jugador.setDx(jugador.getDx() * DASH_SPEED_MULT);
                    jugador.setDy(jugador.getDy() * DASH_SPEED_MULT);
                }
                dashFrames++;
            } else {
                jugador.setDashing(false);
                dashFrames = 0;
                dashCooldown = 30;     //son 60 frames - 1 seg
            }
        }

    }

    private void collisionDetection() {

        screenEdgesCollision();
        camionCollision();
        checkBasuraCollisions();
        playerCollision();

    }

    private void screenEdgesCollision() {

        //En las esquinas puede hacer mas de una de estas condiciones al mismo tiempo. Dejan de ser else-if y se vuelven
        //solo if's.

        if(jugador.getX() + jugador.getDx() < 0){
            jugador.setX(0);
            jugador.setHitboxX(0);
            jugador.setDx(0);
        }

        if(jugador.getY() + jugador.getDy() < 0){
            jugador.setY(0);
            jugador.setHitboxY(jugador.getHitboxHeight());
            jugador.setDy(0);
        }

        if(jugador.getX() + jugador.getDx() > Main.WIDTH - ImageLoader.paradoArriba.getWidth()){
            jugador.setX(Main.WIDTH - jugador.getWidth());
            jugador.setHitboxX(Main.WIDTH - jugador.getHitboxWidth());
            jugador.setDx(0);
        }

        if(jugador.getY() + jugador.getDy() > Main.HEIGHT - jugador.getHeight()) { //- ImageLoader.paradoArriba.getHeight()){
            jugador.setY(Main.HEIGHT - jugador.getHeight());
            jugador.setHitboxY(Main.HEIGHT - jugador.getHitboxHeight());
            jugador.setDy(0);
        }

    }

    private void camionCollision() {

        //Esto es:  Se verifica si el camion llegaria a chocar con el jugador si el camion se moviera, en caso de no suceda
        //se ejecuta su codigo de movimiento y tambien se mueve el mapa.

        if(jugador.collisionsWith(camion.getHitboxX() + camion.getDx(), camion.getHitboxY() + camion.getDy(),
                                      camion.getHitboxWidth(), camion.getHitboxHeight()) == 0                    ) {

            camion.move();
            boteAzul.move();
            bg.setBackgroundX( -camion.getDistance() );
        } else if(getCollisionDirection(camion, jugador) != Direccion.izquierda) {
            camion.move();
            boteAzul.move();
            bg.setBackgroundX( -camion.getDistance() );
        }
    }

    public void checkBasuraCollisions() {
        arrayBasura.getArrayBasura().forEach(basura -> {

            //Colision para detectar si el jugador puede o no recoger la basura en cuestion.
            if (basura.nextTo(jugador.getHitboxX(), jugador.getHitboxY(), jugador.getWidth(), jugador.getHeight(), Player.SPEED) == 1) {
                basura.setNextToPlayer(true);
            } else {
                basura.setNextToPlayer(false);
            }

            if(jugador.collisionsWith(basura.getHitboxX() + basura.getDx(), basura.getHitboxY() + basura.getDy(),
                                          basura.getHitboxWidth(), basura.getHitboxHeight()) == 0 || basura.isMoving())
            {
                basura.move();
            }

            if(basura.collisionsWith(jugador.getHitboxX() + jugador.getDx(), jugador.getHitboxY() + jugador.getDy(),
                                        jugador.getHitboxWidth(), jugador.getHitboxHeight()) == 1 ) {

                directionalCollisionValidation(jugador, basura);
            }

        });
    }

    private void playerCollision() {

        /*
        Colisiones del JUGADOR con otra cosa.
        Las colisiones con la basura deben hacerse en el mismo metodo de la basura para evitar recorrer demasiadas
        veces el array de basuras.
         */

        if(camion.collisionsWith(jugador.getHitboxX() + jugador.getDx(),jugador.getHitboxY() + jugador.getDy(),
                jugador.getHitboxWidth(), jugador.getHitboxHeight()) == 1) {

            directionalCollisionValidation(jugador, camion);
        }

        if(jugador.isCargandoBasura()) {
            //Aqui irian todas las verificaciones de si esta depositando la basura en el vagon correcto.

            if (boteAzul.collisionsWith(jugador.getHitboxX(), jugador.getHitboxY(), jugador.getHitboxWidth(),
                    jugador.getHitboxHeight()) == 1) {

                if(jugador.getBasura() instanceof BasuraPlastico)
                {   // si la basura que lleva es plastico obtenr punto y gasolina
                    accionCollisionBoteObtenerPunto(jugador.getBasura());
                }
                else {
                    // si l a basura no es plastico se pierde punto y gasolina
                    accionCollisionBotePierdePunto(jugador.getBasura());
                }
            }
        }

    }

    //e1 es el que se mueve, e2 es con quien quieres verificar desde que direccion se le ha acercado el e1.

    public Direccion getCollisionDirection(MovingIsoEntity e1, MovingIsoEntity e2) {

        if(e1.getHitboxX() + e1.getDx() <= e2.getHitboxX() + e2.getHitboxWidth() &&
           e1.getHitboxY() + e1.getHitboxHeight() >= e2.getHitboxY() &&
           e1.getHitboxY() <= e2.getHitboxY() + e2.getHitboxHeight()) {
            //Si choca por la derecha
            return Direccion.derecha;
        } else if(e1.getHitboxX() + e1.getHitboxWidth() + e1.getDx() >= e2.getHitboxX() &&
                e1.getHitboxY() + e1.getHitboxHeight() >= e2.getHitboxY() &&
                e1.getHitboxY() <= e2.getHitboxY() + e2.getHitboxHeight()) {
            //Si choca por la izquierda
            return Direccion.izquierda;
        } else if(e1.getHitboxY() + e1.getDy() <= e2.getHitboxY() + e2.getHitboxHeight() &&
                e1.getHitboxX() + e1.getHitboxWidth() >= e2.getHitboxX() &&
                e1.getHitboxX() <= e2.getHitboxX() + e2.getHitboxWidth()) {
            //Si choca por abajo
            return Direccion.abajo;
        } else if(e1.getHitboxY() + e1.getHitboxHeight() + e1.getDy() >= e2.getHitboxY() &&
                e1.getHitboxX() + e1.getHitboxWidth() >= e2.getHitboxX() &&
                e1.getHitboxX() <= e2.getHitboxX() + e2.getHitboxWidth()) {
            //Si choca por arriba
            return Direccion.arriba;
        }

        return null;
    }

    //e1 choca con e2
    private void directionalCollisionValidation(MovingIsoEntity e1, MovingIsoEntity e2) {
        Direccion direccionDeColision = getCollisionDirection(e1,e2);

        if(direccionDeColision == Direccion.derecha || direccionDeColision == Direccion.izquierda) {
            e1.setDx(0);
        }

        if(direccionDeColision == Direccion.arriba || direccionDeColision == Direccion.abajo) {
            e1.setDy(0);
        }

    }

    private void accionCollisionBoteObtenerPunto(Basura basura)
    {
        remove.add(basura);
        camion.setGasolina(camion.getGasolina() + 30);
        puntaje++;
        jugador.setCargandoBasura(false);
        jugador.setBasura(null);
    }

    private void accionCollisionBotePierdePunto(Basura basura)
    {
        remove.add(basura);
        camion.setGasolina(camion.getGasolina() - 30);
        puntaje--;
        jugador.setCargandoBasura(false);
        jugador.setBasura(null);
    }

    private void updateEntitiesInScreen() {
        //Lista de elimianr (si es size de la lista eliminar se elimina)
        if(remove.size()>0)
        {
            arrayBasura.getArrayBasura().removeAll(remove);
            arrayEntidad.removeAll(remove);
            remove.clear();
        }
    }

    private void updateGameState() {
        if(stateGame == StateGame.playing) {

            if(camion.getGasolina()<0)
            {
                setStateGame(StateGame.gameOver);
            }
            /*
            if(bg.getBackgroundX() < -bg.getGameBg(1).getWidth() + WIDTH) {
                setStateGame(StateGame.gameOver);
            }

            */

        }
    }

    public void updateGraphic(GraphicsContext gc,double t)
    {
        gc.clearRect(0,0,WIDTH,HEIGHT);

        if(stateGame==StateGame.menu)
        {
            gc.drawImage(ImageLoader.caminaderecho.getFrame(t),600,400,60,60);
        }
        else if(stateGame == StateGame.playing)
        {
           bg.paintBackground(gc);
           arrayEntidad.forEach(objeto->
           {
               double objetoX = (objeto.getX()+bg.getBackgroundX() < 0 && !intro) ? 0:objeto.getX()+bg.getBackgroundX();

               if(objeto instanceof Player) paintPlayer(gc,t);
               else if(objeto instanceof BasuraBotella) gc.drawImage(ImageLoader.spritePlastico,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraManzana) gc.drawImage(ImageLoader.spriteManzana,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraPeriodico) gc.drawImage(ImageLoader.spritePeriodico,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraBanana) gc.drawImage(ImageLoader.spriteBanana,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraSandia) gc.drawImage(ImageLoader.spriteSandia,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraBolaPapel) gc.drawImage(ImageLoader.spriteBolaPapel,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraPapelAvion) gc.drawImage(ImageLoader.spritePapelAvion,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraFoco) gc.drawImage(ImageLoader.spriteFoco,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraVentanaRoto) gc.drawImage(ImageLoader.spriteVentanaRoto,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());
               else if(objeto instanceof BasuraBotellaRoto) gc.drawImage(ImageLoader.spriteBotellaRoto,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());


               else if (objeto instanceof Camion) gc.drawImage(ImageLoader.spriteCamion, (intro)? objetoX:objeto.getX(),objeto.getY(), objeto.getWidth(), objeto.getHeight());
               else if(objeto instanceof BoteBasura) gc.drawImage(ImageLoader.spriteBoteAzul,(intro)? objetoX:objeto.getX(),objeto.getY(),objeto.getWidth(),objeto.getHeight());

               /*
               *    Esto es para verificar en que lugar estan las hitbox, no lo quiten hasta la entrega.
                */

               gc.setStroke(Color.BLACK);
               gc.strokeRect(objeto.getX(), objeto.getY(), objeto.getWidth(), objeto.getHeight());

               if(objeto instanceof MovingIsoEntity) {
                   MovingIsoEntity b = (MovingIsoEntity) objeto;

                   gc.setStroke(Color.RED);
                   gc.strokeRect(b.getHitboxX(), b.getHitboxY(), b.getHitboxWidth(), b.getHitboxHeight());
               }

               //Hasta aqui
           });

           if(!intro)
                showProgressBar(gc);
        }



         if(stateGame==StateGame.gameOver)
        {
            texto.setText("Has perdido!!!!!!!!!!");
        }

    }

    public void inicializaBotonMenu()
    {
        botonJugar.setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override
            public void handle(TouchEvent event) {
                setStateGame(StateGame.playing);
                addComponet();

            }
        });

        botonJugar.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                setStateGame(StateGame.playing);
                addComponet();
            }
        });

        botonInstruccion.setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override
            public void handle(TouchEvent event) {
                //Main.setStateGame(StateGame.playing);
            }
        });

        botonInstruccion.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //   Main.setStateGame(StateGame.playing);
            }
        });

        botonSalir.setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override
            public void handle(TouchEvent event) {
                System.exit(0);
            }
        });

        botonSalir.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.exit(0);
            }
        });
    }

    private void showProgressBar(GraphicsContext gc){

        double xPos = (WIDTH/2)-75;     //Obtener la coordenada x donde inicia la barra
        gc.setStroke(Color.BLACK);
        gc.strokeRect(xPos,HEIGHT-25,150,10);
        gc.setFill(Color.WHITE);

        double progress = -(bg.getBackgroundX()*100)/bg.getGameBg(1).getWidth();  //  Regla de 3 para % de progreso del nivel
        System.out.printf("%.0f\r", progress);

        if(progress >= 80){     //Si se llega al 80% del mapa
            if(bg.getCurrentMap()+1 < 4){   //Si no esta en el ultimo mapa
                bg.setCurrentMap(bg.getCurrentMap()+1); //Pasar al siguiente
                intro = true;
            }
            else{           //Si esta en el ultimo mapa
                setStateGame(StateGame.gameOver);   //terminar el juego
            }

            camion.setDistance(0);
            bg.reset();
        }
        double indicator = (progress*2)+xPos;          //  Regla de 3 para posicion del indicador en la barra
        gc.fillRect( (Math.min(indicator, xPos + 150)), HEIGHT-35,10,30);
    }

    public void paintPlayer(GraphicsContext gc,double t)
    {
        double playerX = (jugador.getX()+bg.getBackgroundX() < 0 && !intro) ? 0:jugador.getX()+bg.getBackgroundX();

        if (Direccion.abajo==jugador.getDireccion()) {
            if(jugador.getDy()!=0)gc.drawImage(ImageLoader. caminaAbajo.getFrame(t),(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
            else gc.drawImage(ImageLoader.paradoAbajo,(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
        }
        else if(jugador.getDireccion()== Direccion.arriba)
        {
            if (jugador.getDy()!=0) gc.drawImage(ImageLoader.caminaArriba.getFrame(t),(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
            else gc.drawImage(ImageLoader.paradoArriba,(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
        }
        else if(jugador.getDireccion()== Direccion.derecha)
        {
            if (jugador.getDx()!=0) {
                gc.drawImage(ImageLoader.caminaderecho.getFrame(t),(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());

            }

            else {
                gc.drawImage(ImageLoader.paradoDerecho,(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
            }

        }
        else if(jugador.getDireccion()== Direccion.izquierda)
        {
            if(jugador.getDx()!=0) {
                gc.drawImage(ImageLoader.caminaIzquierda.getFrame(t), (intro)? playerX:jugador.getX(), jugador.getY(), jugador.getWidth(), jugador.getHeight());
            }
            else {
                gc.drawImage(ImageLoader.paradoIzquierda,(intro)? playerX:jugador.getX(),jugador.getY(),jugador.getWidth(),jugador.getHeight());
            }
        }
    }


    public static Player getJugador() {
        return jugador;
    }

    public static void setJugador(Player jugador) {
        Main.jugador = jugador;
    }

    public static ArrayBasura getArrayBasura() {
        return arrayBasura;
    }
    public static Camion getCamion() {
        return camion;
    }

    public static void setCamion(Camion camion) {
        Main.camion = camion;
    }
    public static StateGame getStateGame() {
        return stateGame;
    }

    public static void setStateGame(StateGame stateGame) {
        Main.stateGame = stateGame;
    }



    public static void main(String[] args) {
        launch(args);
    }
}
