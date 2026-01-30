Project context (LLM notes)

Entry point and loop
- Main -> MyFrame -> GamePanel
- GamePanel owns GameManager, ticks on Swing Timer (~60 FPS)
- GameManager updates state and draws everything

Core entities
- Squad -> list of PlayerSoldier (main + followers)
- Enemy -> pathfinds to player via Grid + Pathfinder
- Bullet -> simple projectile, damages Enemy / Obstacle
- Grenade -> throwable explosive with cooldown/limit
- Bonus -> drops on enemy death (health or new soldier)
- Obstacle -> destructible boxes, registered in Grid
- Particle -> explosion effect
- Camera -> offsets + shake

Controls (current)
- WASD / arrows: move main soldier
- LMB hold: shooting toward mouse
- G: grenade
- P: pause
- O: god mode
- I: show enemy paths

Resources and build
- Images are in data/ (packed into jar by scripts/build_jar.cmd)
- Player/enemy skins: data/player_1.png, data/player_2.png, data/enemy_1.png..enemy_3.png
- Build output: build/SuperPuperShooter.jar
